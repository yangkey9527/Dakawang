package com.dailycheckin.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records WHERE date = :date")
    fun observeByDate(date: String): Flow<List<CheckinRecord>>

    @Query("SELECT * FROM records WHERE date = :date")
    suspend fun getByDate(date: String): List<CheckinRecord>

    @Query("SELECT * FROM records WHERE taskId = :taskId AND date = :date")
    suspend fun get(taskId: Long, date: String): CheckinRecord?

    @Query("SELECT * FROM records WHERE taskId = :taskId AND date = :date")
    fun observe(taskId: Long, date: String): Flow<CheckinRecord?>

    @Upsert
    suspend fun upsert(record: CheckinRecord)

    @Upsert
    suspend fun upsertAll(records: List<CheckinRecord>)

    @Query("DELETE FROM records WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: Long)

    @Query("SELECT COUNT(*) FROM records WHERE date = :date AND status = :status")
    fun observeCountByStatus(date: String, status: Int): Flow<Int>
}
