package com.sujal.expensetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sujal.expensetracker.data.entity.LendingEntity

@Dao
interface LendingDao {

    @Query("SELECT * FROM lending ORDER BY isSettled ASC, dateMillis DESC")
    fun getAll(): LiveData<List<LendingEntity>>

    @Insert
    suspend fun insert(entity: LendingEntity)

    @Update
    suspend fun update(entity: LendingEntity)

    @Delete
    suspend fun delete(entity: LendingEntity)
}
