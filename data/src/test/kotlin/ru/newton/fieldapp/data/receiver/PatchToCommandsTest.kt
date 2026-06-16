package ru.newton.fieldapp.data.receiver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.domain.receiver.GeoidSetting
import ru.newton.fieldapp.domain.receiver.InputConfig
import ru.newton.fieldapp.domain.receiver.OutputMessageConfig
import ru.newton.fieldapp.domain.receiver.OutputStreamConfig
import ru.newton.fieldapp.domain.receiver.ReceiverConfigPatch
import ru.newton.fieldapp.domain.receiver.RoverMode
import ru.newton.fieldapp.domain.receiver.StreamTarget

class PatchToCommandsTest {
    @Test
    fun `empty patch produces empty list`() {
        val out = PatchToCommands.build(ReceiverConfigPatch())
        assertTrue(out.isEmpty())
    }

    @Test
    fun `rover mode emits mode set rover`() {
        val out = PatchToCommands.build(ReceiverConfigPatch(roverMode = RoverMode.ROVER))
        assertEquals(1, out.size)
        assertEquals("mode set rover", out.first().command)
    }

    @Test
    fun `survey mask emits survey set mask command`() {
        val out = PatchToCommands.build(ReceiverConfigPatch(surveyMaskDeg = 10))
        assertEquals("survey set mask 10", out.first().command)
    }

    @Test
    fun `bluetooth input emits input set bluetooth`() {
        val out = PatchToCommands.build(ReceiverConfigPatch(input = InputConfig.Bluetooth))
        assertEquals("input set bluetooth", out.first().command)
    }

    @Test
    fun `geoid setting maps to coordsystem set geoid`() {
        assertEquals(
            "coordsystem set geoid egm86",
            PatchToCommands.build(ReceiverConfigPatch(geoid = GeoidSetting.EGM86)).first().command,
        )
        assertEquals(
            "coordsystem set geoid wgs84",
            PatchToCommands.build(ReceiverConfigPatch(geoid = GeoidSetting.WGS84)).first().command,
        )
    }

    @Test
    fun `output messages always start with output clear message`() {
        val patch = ReceiverConfigPatch(
            outputMessages = listOf(
                OutputMessageConfig(source = "M", type = "GPGGA", rate = "1HZ", format = "A"),
            ),
        )
        val out = PatchToCommands.build(patch)
        assertEquals("output clear message", out.first().command)
        assertEquals("output add message M GPGGA 1HZ A", out[1].command)
    }

    @Test
    fun `output streams use U+2502 source separator not ASCII pipe`() {
        val patch = ReceiverConfigPatch(
            outputStreams = listOf(
                OutputStreamConfig(sources = listOf("M", "R1"), target = StreamTarget.Bluetooth),
            ),
        )
        val out = PatchToCommands.build(patch)
        val streamCmd = out.last().command
        // The CLAUDE.md autopilot rejection: ASCII pipe '|' between sources is a bug.
        assertTrue('│' in streamCmd) { "expected U+2502 in: $streamCmd" }
        assertTrue('|' !in streamCmd) { "ASCII pipe leaked into: $streamCmd" }
    }

    @Test
    fun `ntrip input embeds host port endpoint login pass`() {
        val patch = ReceiverConfigPatch(
            input = InputConfig.NtripClient(
                host = "rtk.example.ru",
                port = 2101,
                endpoint = "MOSCOW_NETWORK",
                login = "user1",
                password = "secret",
            ),
        )
        val out = PatchToCommands.build(patch)
        val cmd = out.first().command
        assertTrue(cmd.startsWith("input set ntripclient ")) { cmd }
        assertTrue("rtk.example.ru" in cmd)
        assertTrue("MOSCOW_NETWORK" in cmd)
        assertTrue("secret" in cmd)
        assertTrue(cmd.endsWith("gppga")) { cmd }
    }

    @Test
    fun `gsm ntrip input emits input set gsmntripclient for receiver-modem NTRIP`() {
        val patch = ReceiverConfigPatch(
            input = InputConfig.GsmNtripClient(
                host = "95.163.249.164",
                port = 2103,
                endpoint = "NEAREST_RTCM32",
                login = "user1",
                password = "secret",
            ),
        )
        val cmd = PatchToCommands.build(patch).first().command
        assertTrue(cmd.startsWith("input set gsmntripclient ")) { cmd }
        assertTrue("95.163.249.164" in cmd)
        assertTrue("NEAREST_RTCM32" in cmd)
        assertTrue(cmd.endsWith("gppga")) { cmd }
    }
}
