package com.sujal.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object PaymentMethod {
    const val CASH = "CASH"
    const val ONLINE = "ONLINE"
}

@Entity(tableName = "lending")
data class LendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Double,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false,
    /** How the money was originally handed over — PaymentMethod.CASH or PaymentMethod.ONLINE. */
    val paymentMethod: String = PaymentMethod.CASH,
    /** Running total repaid so far. Outstanding = amount - amountPaid. */
    val amountPaid: Double = 0.0
)
