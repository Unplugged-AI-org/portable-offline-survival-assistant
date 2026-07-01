package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.InstalledMap
import ai.unplugged.posa.data.model.Waypoint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PosaDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun migrationFromOneToTwoPreservesExistingDataAndAddsInstalledMaps() = runTest {
        createVersionOneDatabase()

        val database = Room.databaseBuilder(
            context,
            PosaDatabase::class.java,
            TEST_DATABASE_NAME,
        ).addMigrations(PosaDatabase.MIGRATION_1_2).build()

        try {
            val repositories = database.repositories()
            assertEquals(LEGACY_WAYPOINT, repositories.waypoints.get(LEGACY_WAYPOINT.id))

            val installedMap = InstalledMap(
                id = "map-area-test",
                displayName = "Local Area",
                fileName = "local-area.map",
                filePath = "/local/maps/local-area.map",
                byteSize = 42_000L,
                isEnabled = true,
                importedAtEpochMillis = NOW,
                updatedAtEpochMillis = NOW,
            )
            repositories.installedMaps.save(installedMap)

            assertEquals(listOf(installedMap), repositories.installedMaps.listEnabled())
        } finally {
            database.close()
        }
    }

    private fun createVersionOneDatabase() {
        val databaseFile = context.getDatabasePath(TEST_DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.beginTransaction()
            try {
                createVersionOneSchema(database)
                database.execSQL(
                    """
                    INSERT INTO waypoints (
                        id, name, latitude, longitude, elevation_meters, notes,
                        created_at_epoch_millis, updated_at_epoch_millis
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        LEGACY_WAYPOINT.id,
                        LEGACY_WAYPOINT.name,
                        LEGACY_WAYPOINT.latitude,
                        LEGACY_WAYPOINT.longitude,
                        LEGACY_WAYPOINT.elevationMeters,
                        LEGACY_WAYPOINT.notes,
                        LEGACY_WAYPOINT.createdAtEpochMillis,
                        LEGACY_WAYPOINT.updatedAtEpochMillis,
                    ),
                )
                database.version = 1
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }

    private fun createVersionOneSchema(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `waypoints` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `elevation_meters` REAL,
                `notes` TEXT,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_waypoints_name` ON `waypoints` (`name`)")
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_waypoints_created_at_epoch_millis`
            ON `waypoints` (`created_at_epoch_millis`)
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `breadcrumb_trails` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `started_at_epoch_millis` INTEGER NOT NULL,
                `ended_at_epoch_millis` INTEGER,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_breadcrumb_trails_started_at_epoch_millis`
            ON `breadcrumb_trails` (`started_at_epoch_millis`)
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `breadcrumb_points` (
                `id` TEXT NOT NULL,
                `trail_id` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `accuracy_meters` REAL,
                `recorded_at_epoch_millis` INTEGER NOT NULL,
                `sequence_number` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`trail_id`) REFERENCES `breadcrumb_trails`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_breadcrumb_points_trail_id` ON `breadcrumb_points` (`trail_id`)")
        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_breadcrumb_points_trail_id_sequence_number`
            ON `breadcrumb_points` (`trail_id`, `sequence_number`)
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_breadcrumb_points_recorded_at_epoch_millis`
            ON `breadcrumb_points` (`recorded_at_epoch_millis`)
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checklists` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `is_archived` INTEGER NOT NULL,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_checklists_title` ON `checklists` (`title`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_checklists_is_archived` ON `checklists` (`is_archived`)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checklist_items` (
                `id` TEXT NOT NULL,
                `checklist_id` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `details` TEXT,
                `position` INTEGER NOT NULL,
                `is_checked` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`checklist_id`) REFERENCES `checklists`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_checklist_id` ON `checklist_items` (`checklist_id`)")
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_checklist_items_checklist_id_position`
            ON `checklist_items` (`checklist_id`, `position`)
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gear_items` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT,
                `quantity` INTEGER NOT NULL,
                `condition` TEXT,
                `notes` TEXT,
                `is_available` INTEGER NOT NULL,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_gear_items_name` ON `gear_items` (`name`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_gear_items_category` ON `gear_items` (`category`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_gear_items_is_available` ON `gear_items` (`is_available`)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `packs` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `version` TEXT NOT NULL,
                `category` TEXT,
                `description` TEXT,
                `pack_type` TEXT NOT NULL,
                `license` TEXT NOT NULL,
                `source_name` TEXT,
                `source_url` TEXT,
                `author` TEXT,
                `last_reviewed_epoch_millis` INTEGER,
                `review_status` TEXT NOT NULL,
                `installed_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                `is_bundled` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_packs_title` ON `packs` (`title`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_packs_pack_type` ON `packs` (`pack_type`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_packs_review_status` ON `packs` (`review_status`)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `provenance` (
                `id` TEXT NOT NULL,
                `source_title` TEXT NOT NULL,
                `source_url` TEXT,
                `citation` TEXT,
                `license` TEXT,
                `review_status` TEXT NOT NULL,
                `reviewed_by` TEXT,
                `reviewed_at_epoch_millis` INTEGER,
                `notes` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `guide_cards` (
                `id` TEXT NOT NULL,
                `pack_id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `summary` TEXT NOT NULL,
                `body_markdown` TEXT NOT NULL,
                `warnings` TEXT,
                `sort_order` INTEGER NOT NULL,
                `provenance_id` TEXT,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`provenance_id`) REFERENCES `provenance`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_guide_cards_pack_id` ON `guide_cards` (`pack_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_guide_cards_category` ON `guide_cards` (`category`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_guide_cards_provenance_id` ON `guide_cards` (`provenance_id`)")
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_guide_cards_pack_id_sort_order`
            ON `guide_cards` (`pack_id`, `sort_order`)
            """.trimIndent(),
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `field_notes` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                `latitude` REAL,
                `longitude` REAL,
                `waypoint_id` TEXT,
                `checklist_id` TEXT,
                `guide_card_id` TEXT,
                `gear_item_id` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`waypoint_id`) REFERENCES `waypoints`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`checklist_id`) REFERENCES `checklists`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`guide_card_id`) REFERENCES `guide_cards`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`gear_item_id`) REFERENCES `gear_items`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_field_notes_created_at_epoch_millis`
            ON `field_notes` (`created_at_epoch_millis`)
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_field_notes_waypoint_id` ON `field_notes` (`waypoint_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_field_notes_checklist_id` ON `field_notes` (`checklist_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_field_notes_guide_card_id` ON `field_notes` (`guide_card_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_field_notes_gear_item_id` ON `field_notes` (`gear_item_id`)")
    }

    private companion object {
        const val TEST_DATABASE_NAME = "posa-migration-test.db"
        const val NOW = 1_800_000_000_000L

        val LEGACY_WAYPOINT = Waypoint(
            id = "legacy-waypoint",
            name = "Legacy Trailhead",
            latitude = 43.7384,
            longitude = 7.4246,
            elevationMeters = null,
            notes = "Created before installed maps existed.",
            createdAtEpochMillis = NOW,
            updatedAtEpochMillis = NOW,
        )
    }
}
