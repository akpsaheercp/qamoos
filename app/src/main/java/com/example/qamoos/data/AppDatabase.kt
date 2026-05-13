package com.example.qamoos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities = [DictionaryInfo::class, HistoryEntry::class, FavoriteEntry::class], version = 27, exportSchema = false)
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

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE dictionary_info SET display_name = 'English Malayalam' WHERE table_name = 'English'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'മലയാളം' WHERE table_name = 'Malayalam'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'അറബി മലയാളം' WHERE table_name = 'Arabic'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'تاج العروس' WHERE table_name = 'taj'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'لسان العرب' WHERE table_name = 'lisanularab'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'أساس البلاغة' WHERE table_name = 'asaas'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الغني' WHERE table_name = 'mujam_ghani'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الصحاح' WHERE table_name = 'mujamul_shihah'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'المعجم الوسيط' WHERE table_name = 'mujamul_waseet'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'القاموس المحيط' WHERE table_name = 'qamoos_moheet'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'الصحاح في اللغة' WHERE table_name = 'sehah'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الأعلام' WHERE table_name = 'aalam'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الأفعال' WHERE table_name = 'afaal'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'المعجم الطبي' WHERE table_name = 'altibi'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'مجمع الأمثال' WHERE table_name = 'amthal'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم البلاغة' WHERE table_name = 'blaghah_dict'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم البلدان' WHERE table_name = 'boldan'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الفروق' WHERE table_name = 'frooq'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم المعاني' WHERE table_name = 'maany'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'مقاييس اللغة' WHERE table_name = 'maqayys'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'المصباح المنير' WHERE table_name = 'misbah'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'اللغة المعاصرة' WHERE table_name = 'muasirah'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'مفردات القرآن' WHERE table_name = 'mufradatquran'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم المترادفات' WHERE table_name = 'mutradef'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم النحو' WHERE table_name = 'nahwa'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'النهاية في غريب الحديث' WHERE table_name = 'nehayah'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'المعجم الفقهي' WHERE table_name = 'phikhy1'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الصرف' WHERE table_name = 'sarf'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'كتاب التعريفات' WHERE table_name = 'tarefaat'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الأعشاب' WHERE table_name = 'aashab'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الملابس' WHERE table_name = 'clothes'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم المعالم' WHERE table_name = 'maalem'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم المعاني' WHERE table_name = 'maany_dict'")
                db.execSQL("UPDATE dictionary_info SET display_name = 'معجم الإعراب' WHERE table_name = 'irab'")
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
                .setQueryCoroutineContext(kotlinx.coroutines.Dispatchers.IO)
                .addMigrations(MIGRATION_2_25, MIGRATION_25_26, MIGRATION_26_27)
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
                // Force initialization in a thread-safe way to avoid race conditions
                // in createConnectionManager during first concurrent access.
                try {
                    instance.openHelper.writableDatabase
                } catch (e: Exception) {
                    // Ignore initialization errors here, they will be surfaced during actual use
                }
                instance
            }
        }
    }
}
