package com.sujal.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table (id is always 1) holding the online and cash balances.
 * Both are plain numbers you (or a transaction) set directly — [cashAmount] is not
 * derived from the note/coin breakdown; that breakdown is a separate counting tool
 * you can sync into this number on purpose, whenever you actually count your cash.
 */
@Entity(tableName = "balance")
data class BalanceEntity(
    @PrimaryKey val id: Int = 1,
    val onlineAmount: Double = 0.0,
    val cashAmount: Double = 0.0
)
