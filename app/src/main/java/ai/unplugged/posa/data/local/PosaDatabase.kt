package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.local.dao.BreadcrumbDao
import ai.unplugged.posa.data.local.dao.ChecklistDao
import ai.unplugged.posa.data.local.dao.FieldNoteDao
import ai.unplugged.posa.data.local.dao.GearDao
import ai.unplugged.posa.data.local.dao.GuideCardDao
import ai.unplugged.posa.data.local.dao.InstalledMapDao
import ai.unplugged.posa.data.local.dao.PackDao
import ai.unplugged.posa.data.local.dao.ProvenanceDao
import ai.unplugged.posa.data.local.dao.RetrievalDao
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
import ai.unplugged.posa.data.local.entity.RetrievalChunkEntity
import ai.unplugged.posa.data.local.entity.RetrievalDocumentEntity
import ai.unplugged.posa.data.local.entity.RetrievalEmbeddingModelEntity
import ai.unplugged.posa.data.local.entity.WaypointEntity
import android.content.Context
import android.database.sqlite.SQLiteException
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
        RetrievalEmbeddingModelEntity::class,
        RetrievalDocumentEntity::class,
        RetrievalChunkEntity::class,
    ],
    version = 10,
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
    abstract fun retrievalDao(): RetrievalDao

    companion object {
        const val DATABASE_NAME = "posa.db"
        const val DEFAULT_EMBEDDING_MODEL_ID = "BAAI/bge-small-en-v1.5"
        const val DEFAULT_EMBEDDING_DIMENSION = 384
        const val DEFAULT_EMBEDDING_VECTOR_FORMAT = "float32"
        const val DEFAULT_EMBEDDING_DISTANCE_METRIC = "cosine"
        const val DEFAULT_QUERY_INSTRUCTION = "Represent this sentence for searching relevant passages: "

        fun create(context: Context): PosaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PosaDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
            ).addCallback(RETRIEVAL_FTS_CALLBACK).build()

        fun createInMemory(context: Context): PosaDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PosaDatabase::class.java,
            ).addCallback(RETRIEVAL_FTS_CALLBACK).build()

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `center_latitude` REAL")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `center_longitude` REAL")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `start_zoom_level` INTEGER")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `bounding_box_min_latitude` REAL")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `bounding_box_min_longitude` REAL")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `bounding_box_max_latitude` REAL")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `bounding_box_max_longitude` REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `guide_cards` ADD COLUMN `workflow_tags` TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createRetrievalSchema(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `indexing_status` TEXT NOT NULL DEFAULT 'not_indexed'")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `indexed_feature_count` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `indexed_segment_count` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `installed_maps` ADD COLUMN `index_error` TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `gear_items` ADD COLUMN `weight_kilograms` REAL")
                db.execSQL("ALTER TABLE `gear_items` ADD COLUMN `volume_liters` REAL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `checklist_items` ADD COLUMN `gear_item_id` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_gear_item_id` ON `checklist_items` (`gear_item_id`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val RETRIEVAL_FTS_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedDefaultEmbeddingModel(db)
                createRetrievalFtsSchema(db)
            }
        }

        private fun createRetrievalSchema(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `retrieval_embedding_models` (
                    `model_id` TEXT NOT NULL,
                    `display_name` TEXT NOT NULL,
                    `dimension` INTEGER NOT NULL,
                    `vector_format` TEXT NOT NULL,
                    `distance_metric` TEXT NOT NULL,
                    `query_instruction` TEXT,
                    `passage_instruction` TEXT,
                    `max_input_tokens` INTEGER,
                    `is_active` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    PRIMARY KEY(`model_id`)
                )
                """.trimIndent(),
            )
            seedDefaultEmbeddingModel(db)
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_retrieval_embedding_models_is_active`
                ON `retrieval_embedding_models` (`is_active`)
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `retrieval_documents` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `document_type` TEXT NOT NULL,
                    `publisher` TEXT,
                    `category` TEXT NOT NULL,
                    `hazard_tags` TEXT,
                    `audience_tags` TEXT,
                    `urgency` TEXT,
                    `source_url` TEXT,
                    `source_citation` TEXT,
                    `license` TEXT,
                    `content_hash` TEXT NOT NULL,
                    `corpus_version` TEXT NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `updated_at_epoch_millis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_documents_category` ON `retrieval_documents` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_documents_document_type` ON `retrieval_documents` (`document_type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_documents_publisher` ON `retrieval_documents` (`publisher`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_documents_content_hash` ON `retrieval_documents` (`content_hash`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_documents_corpus_version` ON `retrieval_documents` (`corpus_version`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `retrieval_chunks` (
                    `id` TEXT NOT NULL,
                    `document_id` TEXT NOT NULL,
                    `chunk_ordinal` INTEGER NOT NULL,
                    `source_page_start` INTEGER,
                    `source_page_end` INTEGER,
                    `section_title` TEXT,
                    `heading_path` TEXT,
                    `content` TEXT NOT NULL,
                    `token_count` INTEGER,
                    `category` TEXT NOT NULL,
                    `hazard_tags` TEXT,
                    `audience_tags` TEXT,
                    `urgency` TEXT,
                    `embedding_model_id` TEXT,
                    `embedding_dimension` INTEGER,
                    `embedding_version` TEXT,
                    `embedded_at_epoch_millis` INTEGER,
                    `content_hash` TEXT NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `updated_at_epoch_millis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`document_id`) REFERENCES `retrieval_documents`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`embedding_model_id`) REFERENCES `retrieval_embedding_models`(`model_id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_chunks_document_id` ON `retrieval_chunks` (`document_id`)")
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_retrieval_chunks_document_id_chunk_ordinal`
                ON `retrieval_chunks` (`document_id`, `chunk_ordinal`)
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_chunks_category` ON `retrieval_chunks` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_chunks_urgency` ON `retrieval_chunks` (`urgency`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_chunks_embedding_model_id` ON `retrieval_chunks` (`embedding_model_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_retrieval_chunks_content_hash` ON `retrieval_chunks` (`content_hash`)")

            createRetrievalFtsSchema(db)
        }

        private fun seedDefaultEmbeddingModel(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO `retrieval_embedding_models` (
                    `model_id`, `display_name`, `dimension`, `vector_format`, `distance_metric`,
                    `query_instruction`, `passage_instruction`, `max_input_tokens`,
                    `is_active`, `created_at_epoch_millis`
                ) VALUES (
                    '$DEFAULT_EMBEDDING_MODEL_ID', 'BGE Small English v1.5',
                    $DEFAULT_EMBEDDING_DIMENSION, '$DEFAULT_EMBEDDING_VECTOR_FORMAT',
                    '$DEFAULT_EMBEDDING_DISTANCE_METRIC', '$DEFAULT_QUERY_INSTRUCTION',
                    NULL, 512, 1, 0
                )
                """.trimIndent(),
            )
        }

        private fun createRetrievalFtsSchema(db: SupportSQLiteDatabase) {
            createRetrievalFtsTable(db)
            createRetrievalFtsTriggers(db)
        }

        private fun createRetrievalFtsTable(db: SupportSQLiteDatabase) {
            try {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `retrieval_chunks_fts` USING fts5(
                        `section_title`,
                        `content`,
                        `category`,
                        `hazard_tags`,
                        `audience_tags`,
                        `urgency`,
                        content='retrieval_chunks',
                        content_rowid='rowid'
                    )
                    """.trimIndent(),
                )
            } catch (exception: SQLiteException) {
                if (exception.message?.contains("no such module: fts5") != true) {
                    throw exception
                }
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `retrieval_chunks_fts` USING fts4(
                        `section_title`,
                        `content`,
                        `category`,
                        `hazard_tags`,
                        `audience_tags`,
                        `urgency`,
                        content='retrieval_chunks'
                    )
                    """.trimIndent(),
                )
            }
        }

        private fun createRetrievalFtsTriggers(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `retrieval_chunks_fts_after_insert`
                AFTER INSERT ON `retrieval_chunks` BEGIN
                    INSERT INTO `retrieval_chunks_fts`(
                        `rowid`, `section_title`, `content`, `category`,
                        `hazard_tags`, `audience_tags`, `urgency`
                    ) VALUES (
                        new.rowid, new.section_title, new.content, new.category,
                        new.hazard_tags, new.audience_tags, new.urgency
                    );
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `retrieval_chunks_fts_after_delete`
                AFTER DELETE ON `retrieval_chunks` BEGIN
                    INSERT INTO `retrieval_chunks_fts`(
                        `retrieval_chunks_fts`, `rowid`, `section_title`, `content`,
                        `category`, `hazard_tags`, `audience_tags`, `urgency`
                    ) VALUES (
                        'delete', old.rowid, old.section_title, old.content,
                        old.category, old.hazard_tags, old.audience_tags, old.urgency
                    );
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `retrieval_chunks_fts_after_update`
                AFTER UPDATE ON `retrieval_chunks` BEGIN
                    INSERT INTO `retrieval_chunks_fts`(
                        `retrieval_chunks_fts`, `rowid`, `section_title`, `content`,
                        `category`, `hazard_tags`, `audience_tags`, `urgency`
                    ) VALUES (
                        'delete', old.rowid, old.section_title, old.content,
                        old.category, old.hazard_tags, old.audience_tags, old.urgency
                    );
                    INSERT INTO `retrieval_chunks_fts`(
                        `rowid`, `section_title`, `content`, `category`,
                        `hazard_tags`, `audience_tags`, `urgency`
                    ) VALUES (
                        new.rowid, new.section_title, new.content, new.category,
                        new.hazard_tags, new.audience_tags, new.urgency
                    );
                END
                """.trimIndent(),
            )
        }
    }
}
