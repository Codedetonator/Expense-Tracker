package com.sujal.expensetracker.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.sujal.expensetracker.data.ExpenseRepository
import com.sujal.expensetracker.data.entity.ExpenseEntity
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpensesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExpenseRepository(application)

    val allExpenses: LiveData<List<ExpenseEntity>> = repo.allExpenses

    private fun startOfDay(daysAgo: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val totalToday: LiveData<Double> = allExpenses.map { list ->
        val start = startOfDay()
        list.filter { it.dateMillis >= start }.sumOf { it.amount }
    }

    val totalThisMonth: LiveData<Double> = allExpenses.map { list ->
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        list.filter { it.dateMillis >= startOfMonth }.sumOf { it.amount }
    }

    fun add(description: String, amount: Double, category: String, note: String, method: String, dateMillis: Long) =
        viewModelScope.launch {
            if (amount <= 0 || description.isBlank()) return@launch
            repo.addExpense(
                ExpenseEntity(
                    description = description,
                    amount = amount,
                    category = category,
                    note = note,
                    paymentMethod = method,
                    dateMillis = dateMillis
                )
            )
        }

    fun delete(entity: ExpenseEntity) = viewModelScope.launch {
        repo.deleteExpense(entity)
    }
}
