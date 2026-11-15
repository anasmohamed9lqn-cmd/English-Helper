package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TopicEntity::class, WordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao

    companion object {
        @Volatile
        private var INSTANCE: VocabDatabase? = null

        fun getInstance(context: Context): VocabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VocabDatabase::class.java,
                    "english_helper_vocab.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
