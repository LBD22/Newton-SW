package ru.newton.fieldapp.gnss.command

/**
 * Data sources recognised by the receiver. Тaб. 2.6 in the firmware manual.
 *
 * Join via [NewtonCommandBuilder.SRC_SEP] (U+2502), never ASCII pipe.
 */
enum class NewtonSource(
    val code: String,
) {
    MASTER("M"),
    ROVER1("R1"),
    ROVER2("R2"),
    PPP("PPP"),
    IMU("IMU"),
}

/**
 * Message identifiers for `output add message <source> <type> <rate> <format>`.
 * Full set from Табл. 2.2 of the firmware manual.
 *
 * The same [code] can map to multiple receiver-side ids (e.g. GPGGA is id=1
 * for M|R1|R2|PPP and id=55 for IMU). The receiver disambiguates by source —
 * we just send the code and the source picked by the caller.
 */
enum class NewtonMessageType(
    val code: String,
) {
    // NMEA standard
    GPGGA("GPGGA"),
    GPRMC("GPRMC"),
    GPGLL("GPGLL"),
    GPVTG("GPVTG"),
    GPGSA("GPGSA"),
    GPGSV("GPGSV"),
    GPHDT("GPHDT"),
    GPGST("GPGST"),
    GPZDA("GPZDA"),

    // COMNAV proprietary (ASCII)
    GPTRA("GPTRA"),
    GPNAV("GPNAV"),

    // ORIENT / IMU / heading (mostly tilt-aware messages)
    HEADING("HEADING"),
    HEADING12("HEADING12"),
    GPGGAM12("GPGGAM12"),
    PTNLAVR("PTNLAVR"),
    PTNLPJK("PTNLPJK"),
    PCVMB("PCVMB"),
    IMUDATA("IMUDATA"),
    RAWINS("RAWINS"),
    GPYBM("GPYBM"),
    PTNLVHD("PTNLVHD"),
    GPROT("GPROT"),
    GPGNS("GPGNS"),
    GPGGALONG("GPGGALONG"),
    MBS("MBS"),

    // COMNAV binary (raw observations & navigation data)
    BESTPOS("BESTPOS"),
    BESTVEL("BESTVEL"),
    RANGECMP("RANGECMP"),
    RAWEPHEM("RAWEPHEM"),
    GLORAWEPHEM("GLORAWEPHEM"),
    GALEPHEMERIS("GALEPHEMERIS"),
    BD2RAWEPHEM("BD2RAWEPHEM"),
    IONUTC("IONUTC"),
    GPSEPHEM("GPSEPHEM"),
    BD2EPHEM("BD2EPHEM"),
    BD3EPHEM("BD3EPHEM"),
    BESTXYZ("BESTXYZ"),
    PSRPOS("PSRPOS"),
    REFSTATION("REFSTATION"),
    PSRDOP("PSRDOP"),
    PSRVEL("PSRVEL"),
    RANGE("RANGE"),
    TRACKSTAT("TRACKSTAT"),
    M925("M925"),
    SATMSG("SATMSG"),
    SATVIS("SATVIS"),
    SATXYZ("SATXYZ"),
    TIME("TIME"),
    GLOEPHEMERIS("GLOEPHEMERIS"),

    // Survey master
    SURVEY_MASTER("SURVEY MASTER"),

    // RTCM 3.2
    RTCM_MSM4("RTCM MSM4"),
    RTCM_MSM5("RTCM MSM5"),
    RTCM_MSM6("RTCM MSM6"),
    RTCM_MSM7("RTCM MSM7"),

    // PPP feed
    PPP_BOX("PPP BOX"),
}

/** Rates from Табл. 2.4. */
enum class NewtonRate(
    val code: String,
) {
    NONE("NONE"),
    HZ_20("20HZ"),
    HZ_10("10HZ"),
    HZ_5("5HZ"),
    HZ_2("2HZ"),
    HZ_1("1HZ"),
    SEC_5("5S"),
    SEC_10("10S"),
    SEC_30("30S"),
    SEC_60("60S"),
    SEC_120("120S"),
    ON_NEW("ONNEW"),
    ON_CHANGE("ONCHANGE"),
}

/** Wire format for messages. Табл. 2.2 "Тип" column. */
enum class NewtonFormat(
    val code: String,
) {
    ASCII("A"),
    BINARY("B"),
    ASCII_AND_BINARY("AB"),
}

/** Correction types from Табл. 2.3 for `output set correction <N>`. */
enum class NewtonCorrection(
    val id: Int,
    val label: String,
) {
    NONE(0, "Нет"),
    DGNSS(1, "DGNSS"),
    RTCM_23(2, "RTCM 2.3"),
    RTCM_30(3, "RTCM 3.0"),
    RTCM_32_MSM4(4, "RTCM 3.2 MSM4"),
    RTCM_32_MSM5(5, "RTCM 3.2 MSM5"),
    RTCM_32_MSM6(6, "RTCM 3.2 MSM6"),
    RTCM_32_MSM7(7, "RTCM 3.2 MSM7"),
    RTCM_COMNAV(8, "RTCM ComNav"),
    CMR(9, "CMR"),
    COMNAV(10, "ComNav"),
    RTCM_COMPASS(11, "RTCM COMPASS"),
    RTCM_32_MSM4_UHF(12, "RTCM 3.2 MSM4 UHF"),
    RTCM_32_MSM5_UHF(13, "RTCM 3.2 MSM5 UHF"),
    RTCM_32_MSM6_UHF(14, "RTCM 3.2 MSM6 UHF"),
    RTCM_32_MSM7_UHF(15, "RTCM 3.2 MSM7 UHF"),
}

enum class Geoid(
    val code: String,
) {
    WGS84("wgs84"),
    EGM86("egm86"),
    USER("user"),
}

enum class PppType(
    val code: String,
) {
    RTCM_SSR("rtcmssr"),
    ORIENT("orient"),
    PASS2PASS("pass2pass"),
    SINO("sino"),
    SBAS("sbas"),
    RTK("rtk"),
}

enum class SbasSystem(
    val code: String,
) {
    EGNOS("egnos"),
    WAAS("waas"),
    MSAS("msas"),
    GAGAN("gagan"),
}

/**
 * UHF (radio) modulation protocols from Табл. 2.4 «УКВ Протоколы».
 * Compatibility differs by receiver hardware (Harxon vs U50 vs U70) — the
 * caller is responsible for picking a value the connected radio supports.
 */
enum class UhfProtocol(
    val code: String,
) {
    TRIMTALK("trimtalk"),
    TRIMMK3("trimmk3"),
    TRANSEOT("transeot"),
    MAC("mac"),
    TT450S("tt450s"),
    TRANSPARENT("transparent"),
    SOUTH("south"),
    SATEL("satel"),
    LORA("lora"),
}

/**
 * UHF transmit power levels from Табл. 2.5. Values are sent verbatim — note
 * the sub-watt "0.5w" form which is a legal protocol literal.
 */
enum class UhfPower(
    val code: String,
) {
    HIGH("high"),
    LOW("low"),
    HALF_W("0.5w"),
    ONE_W("1w"),
    TWO_W("2w"),
    MEDIUM("medium"),
}

/** COM-port indices accepted by `input set com<N>` and `output add stream … com<N>`. */
enum class ComPort(val index: Int) {
    COM1(1),
    COM2(2),
}
