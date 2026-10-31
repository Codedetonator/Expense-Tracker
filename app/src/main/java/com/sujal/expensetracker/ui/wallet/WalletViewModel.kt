package com.sujal.expensetracker.ui.wallet

import android.app.Application
import androidx.lifecycle.*
import com.sujal.expensetracker.data.ExpenseRepository
import com.sujal.expensetracker.data.entity.DenominationEntity
import com.sujal.expensetracker.util.STANDARD_DENOMINATIONS
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ExpenseRepository(application)

    val onlineBalance: LiveData<Double> = repo.balance.map { it?.onlineAmount ?: 0.0 }
    val cashBalance: LiveData<Double> = repo.balance.map { it?.cashAmount ?: 0.0 }

    val grandTotal: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        var cash = 0.0
        var online = 0.0
        addSource(cashBalance) { cash = it; value = cash + online }
        addSource(onlineBalance) { online = it; value = cash + online }
    }

    fun setOnlineAmount(amount: Double) = viewModelScope.launch {
        repo.setOnlineAmount(amount)
    }

    fun setCashAmount(amount: Double) = viewModelScope.launch {
        repo.setCashAmount(amount)
    }

    // ---- Note/coin counting tool (separate from the cash balance above) ----

    val denominations: LiveData<List<DenominationEntity>> = repo.allDenominations

    /** Every denomination value to display: the standard list plus any custom ones added. */
    val denominationMap: LiveData<Map<Int, Int>> = denominations.map { list ->
        val existing = list.associate { it.value to it.count }
        val allValues = (STANDARD_DENOMINATIONS + existing.keys).distinct().sortedDescending()
        allValues.associateWith { existing[it] ?: 0 }
    }

    fun setDenominationCount(value: Int, count: Int) = viewModelScope.launch {
        repo.setDenominationCount(value, count)
    }

    /** Saves a whole set of note counts at once (does not touch the cash balance). */
    fun saveCounts(counts: Map<Int, Int>) = viewModelScope.launch {
        counts.forEach { (value, count) -> repo.setDenominationCount(value, count) }
    }

    /** Adds a new note/coin value to track, starting at 0, so it shows up as its own row.
     * Does nothing if that value is already being tracked (won't reset an existing count). */
    fun addCustomDenomination(value: Int) = viewModelScope.launch {
        if (value <= 0) return@launch
        repo.addDenominationIfMissing(value)
    }
}
