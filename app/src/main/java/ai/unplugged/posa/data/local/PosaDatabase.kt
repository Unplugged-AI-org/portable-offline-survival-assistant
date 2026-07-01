package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.dao.BreadcrumbDao
import ai.unplugged.posa.data.local.dao.ChecklistDao
import ai.unplugged.posa.data.local.dao.FieldNoteDao
import ai.unplugged.posa.data.local.dao.GearDao
import ai.unplugged.posa.data.local.dao.GuideCardDao
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
import ai.unplugged.posa.data.local.entity.PackEntity
import ai.unplugged.posa.data.local.entity.ProvenanceEntity
import ai.unplugged.posa.data.local.entity.WaypointEntity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WaypointEntity::class,
        BreadcrumbTrailEntity::class,
        BreadcrumbPointEntity::class,
        FieldNoteEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        GearItemEntity::class,
        PackEntity::class,
        GuideCardEntity::class,
        ProvenanceEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PosaDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun breadcrumbDao(): BreadcrumbDao
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
            ).build()

        fun createInMemory(context: Context): PosaDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PosaDatabase::class.java,
            ).build()
    }
}
