package com.sujal.expensetracker.ui.lending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.sujal.expensetracker.data.ExpenseRepository
import com.sujal.expensetracker.data.entity.LendingEntity
import com.sujal.expensetracker.data.entity.PaymentMethod
import kotlinx.coroutines.launch

class LendingViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExpenseRepository(application)

    val allLending: LiveData<List<LendingEntity>> = repo.allLending

    /** Sum of (amount - amountPaid) across every loan that isn't fully settled yet. */
    val totalOutstanding: LiveData<Double> = allLending.map { list ->
        list.sumOf { (it.amount - it.amountPaid).coerceAtLeast(0.0) }
    }

    /** Lends [amount] to [personName], deducting it from the matching balance. */
    fun add(personName: String, amount: Double, note: String, method: String) = viewModelScope.launch {
        if (amount <= 0) return@launch
        repo.addLending(LendingEntity(personName = personName, amount = amount, note = note, paymentMethod = method))
        adjustBalance(method, -amount)
    }

    /** Records a repayment (full or partial) and deposits it into the matching balance. */
    fun repay(entity: LendingEntity, amount: Double, method: String) = viewModelScope.launch {
        if (entity.isSettled || amount <= 0) return@launch
        val newPaid = entity.amountPaid + amount
        val settled = newPaid >= entity.amount
        repo.updateLending(entity.copy(amountPaid = newPaid, isSettled = settled))
        adjustBalance(method, amount)
    }

    private suspend fun adjustBalance(method: String, delta: Double) {
        if (method == PaymentMethod.ONLINE) repo.adjustOnline(delta) else repo.adjustCash(delta)
    }

    /** Deletes (writes off) the loan without touching any balance. */
    fun delete(entity: LendingEntity) = viewModelScope.launch {
        repo.deleteLending(entity)
    }
}
