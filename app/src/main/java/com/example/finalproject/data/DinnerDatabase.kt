package com.example.finalproject.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Entities
@Entity(tableName = "dinners")
data class Dinner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val attendees: String
)

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val isOnline: Boolean = false
)

@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val category: String,
    val lastUsed: Long = 0
)

// DAO
@Dao
interface DinnerDao {
    // Dinners
    @Query("SELECT * FROM dinners ORDER BY id DESC")
    fun getAllDinners(): Flow<List<Dinner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDinner(dinner: Dinner)

    // Family
    @Query("SELECT * FROM family_members")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember)

    @Query("UPDATE family_members SET isOnline = :status WHERE id = :id")
    suspend fun updateMemberStatus(id: Int, status: Boolean)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMember)

    // Topics
    @Query("SELECT * FROM topics ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomTopic(): Topic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTopics(topics: List<Topic>)
}

// Database
@Database(entities = [Dinner::class, FamilyMember::class, Topic::class], version = 1, exportSchema = false)
abstract class DinnerDatabase : RoomDatabase() {
    abstract fun dinnerDao(): DinnerDao

    companion object {
        @Volatile
        private var Instance: DinnerDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DinnerDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, DinnerDatabase::class.java, "dinner_db")
                    .addCallback(DinnerDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }

    private class DinnerDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Instance?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateTopics(database.dinnerDao())
                }
            }
        }

        suspend fun populateTopics(dao: DinnerDao) {
            val initialTopics = listOf(
                Topic(text = "What was the best part of your day?", category = "Gratitude"),
                Topic(text = "If you could have any superpower, what would it be?", category = "Creative"),
                Topic(text = "What is one goal you want to achieve this week?", category = "Goals"),
                Topic(text = "Tell us a funny joke!", category = "Random")
            )
            dao.insertAllTopics(initialTopics)
        }
    }
}