package com.sujal.expensetracker.util

/** Ordered (category name, emoji) pairs used everywhere a category needs to be picked or shown. */
val EXPENSE_CATEGORIES: List<Pair<String, String>> = listOf(
    "Food" to "🍔",
    "Transport" to "🚗",
    "Shopping" to "🛍️",
    "Health" to "💊",
    "Bills" to "🧾",
    "Entertainment" to "🎬",
    "Education" to "📚",
    "Groceries" to "🛒",
    "Other" to "🔖"
)

fun emojiForCategory(category: String): String =
    EXPENSE_CATEGORIES.firstOrNull { it.first == category }?.second ?: "🔖"
