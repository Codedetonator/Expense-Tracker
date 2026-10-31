package com.sujal.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: String,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    /** PaymentMethod.CASH or PaymentMethod.ONLINE — which balance this was deducted from. */
    val paymentMethod: String = PaymentMethod.CASH
)
