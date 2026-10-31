package com.sujal.expensetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sujal.expensetracker.data.entity.BalanceEntity

@Dao
interface BalanceDao {

    @Query("SELECT * FROM balance WHERE id = 1 LIMIT 1")
    fun get(): LiveData<BalanceEntity?>

    @Query("SELECT * FROM balance WHERE id = 1 LIMIT 1")
    suspend fun getOnce(): BalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BalanceEntity)
}
