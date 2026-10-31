package com.sujal.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per note/coin value (2000, 500, 200, 100, 50, 20, 10, 5, 2, 1, or a custom value).
 * This is a standalone counting tool — not wired into any balance automatically.
 */
@Entity(tableName = "denominations")
data class DenominationEntity(
    @PrimaryKey val value: Int,
    val count: Int
)
