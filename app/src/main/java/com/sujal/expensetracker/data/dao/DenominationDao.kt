package com.sujal.expensetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sujal.expensetracker.data.entity.DenominationEntity

@Dao
interface DenominationDao {

    @Query("SELECT * FROM denominations ORDER BY value DESC")
    fun getAll(): LiveData<List<DenominationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DenominationEntity)

    @Query("SELECT * FROM denominations WHERE value = :value LIMIT 1")
    suspend fun get(value: Int): DenominationEntity?
}
