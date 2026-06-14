package ru.newton.fieldapp.gnss.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.newton.fieldapp.gnss.data.parsers.NmeaParsed
import ru.newton.fieldapp.gnss.data.parsers.SatelliteInView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton aggregator that turns a stream of typed [NmeaParsed] events into a
 * coherent [GnssStatus] snapshot.
 *
 * UI listens via [status]. Updates are atomic per [submit] call: subscribers
 * never see GGA fields from epoch T mixed with GST fields from epoch T-1
 * within a single emission.
 *
 * Threading: relies on [MutableStateFlow.update]'s lock-free CAS loop, so
 * `submit` is safe to call from any coroutine. The producer side typically
 * runs on `Dispatchers.IO`; UI collectors stay on `Dispatchers.Main`.
 */
@Singleton
class GnssStatusStore
    @Inject
    constructor() {
        private val _status = MutableStateFlow(GnssStatus.initial())
        val status: StateFlow<GnssStatus> = _status.asStateFlow()

        // GSV multi-sentence batches arrive in order: msg 1/N, 2/N, ..., N/N.
        // We accumulate satellites in a per-constellation buffer keyed by
        // talker id, replacing the public list when the last sentence of a
        // batch lands. Skyplot UI sees full snapshots, never half-batches.
        private val gsvBuffer = mutableMapOf<String, MutableList<SatelliteInView>>()
        private val gsvLastMessage = mutableMapOf<String, Int>()

        // Lives for the whole app; the singleton never goes away. Watches the
        // snapshot age and flips `isStale` when GGA stops arriving, so the UI and
        // survey logic can tell "frozen last position" from "live position".
        private val watchdogScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        init {
            watchdogScope.launch {
                while (isActive) {
                    delay(STALE_CHECK_INTERVAL_MS)
                    val snapshot = _status.value
                    val ageMs = System.currentTimeMillis() - snapshot.timestampUtc
                    if (snapshot.timestampUtc != 0L && !snapshot.isStale && ageMs > STALE_THRESHOLD_MS) {
                        _status.update { if (it.isStale) it else it.copy(isStale = true) }
                    }
                }
            }
        }

        /** Apply one NMEA parse result. Unknown/error cases are no-ops here — the
         *  dispatcher logs them; the store keeps the last good status. */
        fun submit(parsed: NmeaParsed) {
            when (parsed) {
                is NmeaParsed.Gga -> _status.update { current -> current.applyGga(parsed) }
                is NmeaParsed.Gst -> _status.update { current -> current.applyGst(parsed) }
                is NmeaParsed.Gsa -> _status.update { current -> current.applyGsa(parsed) }
                is NmeaParsed.Tra -> _status.update { current -> current.applyTra(parsed) }
                is NmeaParsed.Vhd -> _status.update { current -> current.applyVhd(parsed) }
                is NmeaParsed.Avr -> _status.update { current -> current.applyAvr(parsed) }
                is NmeaParsed.Gsv -> applyGsv(parsed)
                is NmeaParsed.Unknown, is NmeaParsed.Malformed, is NmeaParsed.ChecksumError -> Unit
            }
        }

        /**
         * Accumulate the per-constellation GSV batch and publish a full
         * skyplot snapshot when the last sentence of the batch arrives.
         * If sentences arrive out of order (rare but possible on noisy
         * radios), we reset the buffer to avoid half-stale state.
         */
        private fun applyGsv(parsed: NmeaParsed.Gsv) {
            val constellation = parsed.satellites.firstOrNull()?.constellation
                ?: return // no satellites = nothing to merge
            val expected = (gsvLastMessage[constellation] ?: 0) + 1
            if (parsed.messageNumber != expected) {
                gsvBuffer[constellation] = mutableListOf()
            }
            gsvLastMessage[constellation] = parsed.messageNumber
            val buf = gsvBuffer.getOrPut(constellation) { mutableListOf() }
            buf += parsed.satellites
            if (parsed.messageNumber == parsed.totalMessages) {
                // Batch complete — replace this constellation's slice in the
                // public list, keep other constellations untouched.
                val mergedAll = _status.value.satellitesInView
                    .filterNot { it.constellation == constellation } + buf.toList()
                // Derive satsVisible from the merged list rather than GSV field 3
                // (totalSatsInView is per-constellation only). This keeps the
                // "Наблюдаемых спутников" counter consistent with the skyplot and
                // sums across all constellations. Previously satsVisible was never
                // set, so GNSS Status showed 0 while the main screen showed satsUsed.
                _status.update { it.copy(satellitesInView = mergedAll, satsVisible = mergedAll.size) }
                gsvBuffer[constellation] = mutableListOf()
                gsvLastMessage[constellation] = 0
            }
        }

        /** Drop accumulated state — used on disconnect. */
        fun reset() { _status.value = GnssStatus.initial() }

        private fun GnssStatus.applyGga(parsed: NmeaParsed.Gga): GnssStatus {
            val fix = mapFixQuality(parsed.fixQuality)
            val noFix = fix == FixQuality.NoFix || parsed.latitude == null
            return copy(
                fix = fix,
                latitude = parsed.latitude,
                longitude = parsed.longitude,
                ellipsoidalHeight = parsed.ellipsoidalHeight,
                geoidSeparation = parsed.geoidSeparation,
                satsUsed = parsed.satsUsed,
                hdop = parsed.hdop ?: hdop,
                correctionAgeSec = parsed.correctionAgeSec,
                // A fresh GGA is the liveness signal — its arrival clears staleness.
                isStale = false,
                timestampUtc = System.currentTimeMillis(),
                // Fix lost → drop the accuracy figures too. Otherwise the last
                // cm-level σ from GST lingers on the strip and the surveyor reads
                // an accuracy that no longer exists.
                sigmaN = if (noFix) null else sigmaN,
                sigmaE = if (noFix) null else sigmaE,
                sigmaH = if (noFix) null else sigmaH,
            )
        }

        private fun GnssStatus.applyGst(parsed: NmeaParsed.Gst): GnssStatus = copy(
            sigmaN = parsed.sigmaLat,
            sigmaE = parsed.sigmaLon,
            sigmaH = parsed.sigmaAlt,
        )

        private fun GnssStatus.applyGsa(parsed: NmeaParsed.Gsa): GnssStatus = copy(
            pdop = parsed.pdop ?: pdop,
            hdop = parsed.hdop ?: hdop,
            vdop = parsed.vdop ?: vdop,
        )

        private fun GnssStatus.applyTra(parsed: NmeaParsed.Tra): GnssStatus = copy(
            headingDeg = parsed.headingDeg,
            pitchDeg = parsed.pitchDeg,
            rollDeg = parsed.rollDeg,
            imuValid = parsed.headingDeg != null,
        )

        // VHD / AVR override TRA when present — dual-antenna heading is more
        // accurate than IMU-only. Keep whatever roll the previous sentence
        // gave us if VHD/AVR don't carry it.
        private fun GnssStatus.applyVhd(parsed: NmeaParsed.Vhd): GnssStatus = copy(
            headingDeg = parsed.headingDeg ?: headingDeg,
            pitchDeg = parsed.pitchDeg ?: pitchDeg,
            rollDeg = parsed.rollDeg ?: rollDeg,
            imuValid = parsed.headingDeg != null || imuValid,
        )

        private fun GnssStatus.applyAvr(parsed: NmeaParsed.Avr): GnssStatus = copy(
            headingDeg = parsed.headingDeg ?: headingDeg,
            // PTNLAVR uses "tilt" as its pitch field name.
            pitchDeg = parsed.tiltDeg ?: pitchDeg,
            rollDeg = parsed.rollDeg ?: rollDeg,
            imuValid = parsed.headingDeg != null || imuValid,
        )

        private companion object {
            // Flip to stale after this long with no GGA; check at this cadence.
            const val STALE_THRESHOLD_MS = 3_000L
            const val STALE_CHECK_INTERVAL_MS = 1_000L
        }

        private fun mapFixQuality(rawGgaField: Int): FixQuality = when (rawGgaField) {
            0 -> FixQuality.NoFix
            1 -> FixQuality.Single
            2 -> FixQuality.DGnss
            4 -> FixQuality.FixedRtk
            5 -> FixQuality.FloatRtk
            in 6..9 -> FixQuality.Ppp(type = "PPP-$rawGgaField")
            else -> FixQuality.NoFix
        }
    }
