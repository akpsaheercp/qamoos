package com.example.qamoos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities = [DictionaryInfo::class, HistoryEntry::class, FavoriteEntry::class], version = 26, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun arabicDao(): ArabicDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_25 = object : Migration(2, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `word` TEXT NOT NULL, `meaning` TEXT NOT NULL, `dictionaryName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `word` TEXT NOT NULL, `meaning` TEXT NOT NULL, `dictionaryName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO dictionary_info (id, table_name, display_name, is_selected, display_order) VALUES (29, 'aashab', 'Mujam al-Aashab', 0, 29)")
                db.execSQL("INSERT OR IGNORE INTO dictionary_info (id, table_name, display_name, is_selected, display_order) VALUES (30, 'clothes', 'Dictionary of Clothes', 0, 30)")
                db.execSQL("INSERT OR IGNORE INTO dictionary_info (id, table_name, display_name, is_selected, display_order) VALUES (31, 'maalem', 'Mujam al-Maalem', 0, 31)")
                db.execSQL("INSERT OR IGNORE INTO dictionary_info (id, table_name, display_name, is_selected, display_order) VALUES (32, 'maany_dict', 'Mujam al-Maany (Dict)', 0, 32)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qamoos_v25_online"
                )
                .createFromAsset("dictionary.db")
                .addMigrations(MIGRATION_2_25, MIGRATION_25_26)
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // This callback ensures the core database is copied from assets if it doesn't exist
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA cache_size = -10000;")
                        db.execSQL("PRAGMA temp_store = MEMORY;")
                        db.execSQL("PRAGMA max_attached_count = 100;")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
