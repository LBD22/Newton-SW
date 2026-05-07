package ru.newton.fieldapp.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Validates the Room auto-migration chain v1 → v10. Runs as an instrumented
 * test on a device/emulator (`./gradlew :data:connectedAndroidTest`).
 *
 * Why this matters: a user upgrading from Sprint 1 (v1) all the way to v10
 * runs nine `AutoMigration` steps in sequence. If any step is wrong the app
 * will crash on first launch after the update. We seed v1 with a row, run
 * the chain, and assert the row survives.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NewtonDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrateAllFromV1ToLatest() {
        // 1. Bootstrap v1 schema and seed one row via raw SQL — entity classes
        //    have moved on by v10, so we can't use the DAO at this point.
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO projects(project_id, name, comment, crs_config_json, created_at_utc, updated_at_utc)
                VALUES (1, 'TestProject', 'seed', '{"presetId":"WGS84"}', 0, 0)
                """.trimIndent(),
            )
        }

        // 2. Run the entire auto-migration chain by opening the DB at the
        //    latest version through the actual Room builder. Room replays
        //    every AutoMigration in order.
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            NewtonDatabase::class.java,
            dbName,
        ).build()
        try {
            // 3. Smoke-test the DAO layer reads the surviving row at v10.
            val all = runBlocking { db.projectDao().observeAll().first() }
            assertEquals(1, all.size)
            assertEquals("TestProject", all.first().name)

            // 4. v10 introduced `layers` — empty list for a project with
            //    no layers exercises the new schema end-to-end.
            val layers = runBlocking { db.layerDao().observeByProject(1L).first() }
            assertTrue(layers.isEmpty())
        } finally {
            db.close()
        }
    }
}
