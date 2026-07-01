package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.dao.BreadcrumbDao
import ai.unplugged.posa.data.local.dao.ChecklistDao
import ai.unplugged.posa.data.local.dao.FieldNoteDao
import ai.unplugged.posa.data.local.dao.GearDao
import ai.unplugged.posa.data.local.dao.GuideCardDao
import ai.unplugged.posa.data.local.dao.InstalledMapDao
import ai.unplugged.posa.data.local.dao.PackDao
import ai.unplugged.posa.data.local.dao.ProvenanceDao
import ai.unplugged.posa.data.local.dao.WaypointDao
import ai.unplugged.posa.data.local.entity.BreadcrumbPointEntity
import ai.unplugged.posa.data.local.entity.BreadcrumbTrailEntity
import ai.unplugged.posa.data.local.entity.ChecklistEntity
import ai.unplugged.posa.data.local.entity.ChecklistItemEntity
import ai.unplugged.posa.data.local.entity.FieldNoteEntity
import ai.unplugged.posa.data.local.entity.GearItemEntity
import ai.unplugged.posa.data.local.entity.GuideCardEntity
import ai.unplugged.posa.data.local.entity.InstalledMapEntity
import ai.unplugged.posa.data.local.entity.PackEntity
import ai.unplugged.posa.data.local.entity.ProvenanceEntity
import ai.unplugged.posa.data.local.entity.WaypointEntity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WaypointEntity::class,
        BreadcrumbTrailEntity::class,
        BreadcrumbPointEntity::class,
        InstalledMapEntity::class,
        FieldNoteEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        GearItemEntity::class,
        PackEntity::class,
        GuideCardEntity::class,
        ProvenanceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PosaDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun breadcrumbDao(): BreadcrumbDao
    abstract fun installedMapDao(): InstalledMapDao
    abstract fun fieldNoteDao(): FieldNoteDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun gearDao(): GearDao
    abstract fun packDao(): PackDao
    abstract fun guideCardDao(): GuideCardDao
    abstract fun provenanceDao(): ProvenanceDao

    companion object {
        const val DATABASE_NAME = "posa.db"

        fun create(context: Context): PosaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PosaDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2).build()

        fun createInMemory(context: Context): PosaDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PosaDatabase::class.java,
            ).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installed_maps` (
                        `id` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `file_name` TEXT NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `byte_size` INTEGER NOT NULL,
                        `is_enabled` INTEGER NOT NULL,
                        `imported_at_epoch_millis` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_maps_display_name` ON `installed_maps` (`display_name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_maps_is_enabled` ON `installed_maps` (`is_enabled`)")
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_installed_maps_imported_at_epoch_millis`
                    ON `installed_maps` (`imported_at_epoch_millis`)
                    """.trimIndent(),
                )
            }
        }
    }
}
