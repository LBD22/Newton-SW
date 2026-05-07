package ru.newton.fieldapp.gnss.ntrip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SourceTableParserTest {
    @Test
    fun `extracts mountpoint info from a typical caster response`() {
        val response =
            """
            SOURCETABLE 200 OK
            Server: NTRIP Caster
            Content-Type: gnss/sourcetable
            Content-Length: 1234

            CAS;rtk.example.ru;2101;Example Caster;Example;0;RUS;55.75;37.62;0.0.0.0;0;
            NET;RTKNET;Example;B;N;none;none;none;none
            STR;MSK_RTCM3;Moscow Centre;RTCM 3.2;1004(1),1006(10),1019,1020;2;GPS+GLO;RTKNET;RUS;55.75;37.62;1;0;Newton;none;B;N;1200;
            STR;SPB_RTCM3;Saint-Petersburg;RTCM 3.2;1004(1);2;GPS+GLO+GAL+BDS;RTKNET;RUS;59.93;30.36;0;1;Newton;none;B;N;1200;
            ENDSOURCETABLE
            """.trimIndent()

        val mounts = SourceTableParser.parse(response)
        assertEquals(2, mounts.size)
        val msk = mounts.first { it.id == "MSK_RTCM3" }
        assertEquals("RTCM 3.2", msk.format)
        assertEquals("RTKNET", msk.network)
        assertEquals("RUS", msk.country)
        assertEquals(55.75, msk.latitude)
        assertEquals(37.62, msk.longitude)
        assertTrue(msk.nmea)
        assertEquals(0, msk.solution)
        val spb = mounts.first { it.id == "SPB_RTCM3" }
        assertEquals(false, spb.nmea)
        assertEquals(1, spb.solution)
        assertEquals("GPS+GLO+GAL+BDS", spb.navSystem)
    }

    @Test
    fun `ignores CAS and NET rows`() {
        val response =
            """
            CAS;a;1;b;c;0;d;0.0;0.0;0.0.0.0;0;
            NET;net;c;B;N;none;none;none;none
            STR;m1;n;F;FD;1;GPS;net;RUS;55;37;0;0;
            """.trimIndent()
        val mounts = SourceTableParser.parse(response)
        assertEquals(1, mounts.size)
        assertEquals("m1", mounts.first().id)
    }

    @Test
    fun `skips malformed rows without aborting`() {
        val response =
            """
            STR;ok;name;F;FD;1;GPS;net;RUS;55;37;0;0;
            STR;short;not enough fields
            STR;ok2;name;F;FD;1;GPS;net;RUS;60;30;1;1;
            """.trimIndent()
        val mounts = SourceTableParser.parse(response)
        assertEquals(2, mounts.size)
        assertNotNull(mounts.firstOrNull { it.id == "ok" })
        assertNotNull(mounts.firstOrNull { it.id == "ok2" })
    }

    @Test
    fun `empty content yields empty list`() {
        assertTrue(SourceTableParser.parse("").isEmpty())
        assertTrue(SourceTableParser.parse("\n\n").isEmpty())
    }
}
