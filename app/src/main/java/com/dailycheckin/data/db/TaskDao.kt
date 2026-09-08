package com.dailycheckin.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CheckinTask>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CheckinTask>

    @Query("SELECT * FROM tasks WHERE enabled = 1 ORDER BY sortOrder ASC, id ASC")
    suspend fun getEnabled(): List<CheckinTask>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<CheckinTask?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): CheckinTask?

    @Upsert
    suspend fun upsert(task: CheckinTask): Long

    @Delete
    suspend fun delete(task: CheckinTask)
}
