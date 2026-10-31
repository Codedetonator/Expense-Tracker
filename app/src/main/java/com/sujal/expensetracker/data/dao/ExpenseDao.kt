package com.sujal.expensetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.sujal.expensetracker.data.entity.ExpenseEntity

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAll(): LiveData<List<ExpenseEntity>>

    @Insert
    suspend fun insert(entity: ExpenseEntity)

    @Delete
    suspend fun delete(entity: ExpenseEntity)
}
