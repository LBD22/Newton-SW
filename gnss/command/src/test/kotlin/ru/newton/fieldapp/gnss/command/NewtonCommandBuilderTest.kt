package ru.newton.fieldapp.gnss.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Locks the on-wire format of [NewtonCommandBuilder] against the firmware
 * manual `reference/OSdoc_command_port_*.docx`. Whitespace, casing and the
 * U+2502 separator are all part of the protocol — drift breaks silently.
 */
class NewtonCommandBuilderTest {
    @Test fun `system save matches doc`() {
        assertEquals("system save", NewtonCommandBuilder.systemSave())
    }

    @Test fun `set command mode on off matches doc`() {
        assertEquals("set command mode on", NewtonCommandBuilder.setCommandModeOn())
        assertEquals("set command mode off", NewtonCommandBuilder.setCommandModeOff())
    }

    @Test fun `eth0 set static carries all six fields in order`() {
        val cmd = NewtonCommandBuilder.eth0SetStatic(
            host = "192.168.1.10",
            gateway = "192.168.1.1",
            mask = "255.255.255.0",
            dns1 = "8.8.8.8",
            dns2 = "1.1.1.1",
            mtu = 1500,
        )
        assertEquals("eth0 set static 192.168.1.10 192.168.1.1 255.255.255.0 8.8.8.8 1.1.1.1 1500", cmd)
    }

    @Test fun `mode set base preserves coordinate ordering B L H`() {
        // Doc example: «mode set base 38.45655 56.46565 120» — B first, L second.
        assertEquals(
            "mode set base 38.45655 56.46565 120.0",
            NewtonCommandBuilder.modeSetBase(38.45655, 56.46565, 120.0),
        )
    }

    @Test fun `output add stream uses U+2502 between sources, never ASCII pipe`() {
        val cmd = NewtonCommandBuilder.outputAddStreamBluetooth(
            listOf(NewtonSource.MASTER, NewtonSource.ROVER1, NewtonSource.IMU),
        )
        assert(!cmd.contains('|')) { "ASCII pipe leaked: $cmd" }
        assertEquals("output add stream M│R1│IMU bluetooth", cmd)
    }

    @Test fun `output add message uses single source, no separator`() {
        val cmd = NewtonCommandBuilder.outputAddMessage(
            source = NewtonSource.ROVER1,
            type = NewtonMessageType.GPGGA,
            rate = NewtonRate.HZ_1,
            format = NewtonFormat.ASCII,
        )
        assertEquals("output add message R1 GPGGA 1HZ A", cmd)
    }

    @Test fun `output set correction emits the numeric id from table 2_3`() {
        assertEquals("output set correction 7", NewtonCommandBuilder.outputSetCorrection(NewtonCorrection.RTCM_32_MSM7))
        assertEquals("output set correction 0", NewtonCommandBuilder.outputSetCorrection(NewtonCorrection.NONE))
    }

    @Test fun `ppp set non-sbas type uses two-arg form`() {
        assertEquals("ppp rtk on", NewtonCommandBuilder.pppSet(PppType.RTK, true))
        assertEquals("ppp rtcmssr off", NewtonCommandBuilder.pppSet(PppType.RTCM_SSR, false))
    }

    @Test fun `ppp sbas form carries SBAS_SYSTEM as the third arg`() {
        assertEquals("ppp sbas on egnos", NewtonCommandBuilder.pppSbas(true, SbasSystem.EGNOS))
        assertEquals("ppp sbas off waas", NewtonCommandBuilder.pppSbas(false, SbasSystem.WAAS))
    }

    @Test fun `input set ntripclient quotes the gppga keyword and login fields`() {
        val cmd = NewtonCommandBuilder.inputSetNtripClient(
            host = "rtk.kosmosnimki.ru",
            port = 2101,
            endpoint = "VRS3M",
            login = "user",
            pass = "p@ss",
        )
        assertEquals("input set ntripclient rtk.kosmosnimki.ru 2101 VRS3M user p@ss gppga", cmd)
    }

    @Test fun `input set uhf carries frequency, protocol, baudrate in order`() {
        assertEquals(
            "input set uhf 461.025 trimtalk 19200",
            NewtonCommandBuilder.inputSetUhf(461.025, UhfProtocol.TRIMTALK, 19200),
        )
    }

    @Test fun `output add stream uhf includes power as the last token`() {
        val cmd = NewtonCommandBuilder.outputAddStreamUhf(
            sources = listOf(NewtonSource.MASTER),
            frequencyMHz = 150.25,
            protocol = UhfProtocol.TRIMTALK,
            baudrate = 19200,
            power = UhfPower.HIGH,
        )
        assertEquals("output add stream M uhf 150.25 trimtalk 19200 high", cmd)
    }

    @Test fun `bluetooth bridge with com keeps the on or off keyword first`() {
        assertEquals(
            "bluetooth bridge on com1 115200",
            NewtonCommandBuilder.bluetoothBridgeCom(true, ComPort.COM1, 115200),
        )
        assertEquals(
            "bluetooth bridge off",
            NewtonCommandBuilder.bluetoothBridge(false),
        )
    }

    @Test fun `output remove uses index from system list`() {
        assertEquals("output remove stream 3", NewtonCommandBuilder.outputRemoveStream(3))
        assertEquals("output remove message 0", NewtonCommandBuilder.outputRemoveMessage(0))
    }

    @Test fun `gsm set quotes apns containing whitespace`() {
        // APN values in real life are bare, but the builder defends against
        // a bad copy-paste with spaces — round-trips through quoteIfNeeded.
        assertEquals(
            "gsm set on \"my apn\" \"backup apn\"",
            NewtonCommandBuilder.gsmSet(true, "my apn", "backup apn"),
        )
    }
}
