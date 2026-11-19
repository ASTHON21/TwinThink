package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DialecticSessionEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.PersonaAvatarEntity

@Database(
    entities = [
        DialecticSessionEntity::class,
        DialecticNodeEntity::class,
        DictionaryTermEntity::class,
        PersonaAvatarEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DialecticDatabase : RoomDatabase() {
    abstract fun dialecticDao(): DialecticDao

    companion object {
        @Volatile
        private var INSTANCE: DialecticDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `persona_avatars` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `expression` TEXT NOT NULL,
                        `fillColorArgb` INTEGER NOT NULL,
                        `strokeColorArgb` INTEGER NOT NULL,
                        `perspective` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `dialectic_nodes` ADD COLUMN `speakerAvatarId` INTEGER")
                db.execSQL("ALTER TABLE `dialectic_nodes` ADD COLUMN `speakerName` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): DialecticDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DialecticDatabase::class.java,
                    "dialectic_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
