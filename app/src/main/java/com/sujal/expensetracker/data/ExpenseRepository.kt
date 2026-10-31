package com.sujal.expensetracker.data

import android.content.Context
import com.sujal.expensetracker.data.entity.BalanceEntity
import com.sujal.expensetracker.data.entity.DenominationEntity
import com.sujal.expensetracker.data.entity.ExpenseEntity
import com.sujal.expensetracker.data.entity.LendingEntity
import com.sujal.expensetracker.data.entity.PaymentMethod

class ExpenseRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val denominationDao = db.denominationDao()
    private val balanceDao = db.balanceDao()
    private val lendingDao = db.lendingDao()
    private val expenseDao = db.expenseDao()

    // Denominations — a standalone counting tool, not wired into any balance automatically.
    val allDenominations = denominationDao.getAll()
    suspend fun setDenominationCount(value: Int, count: Int) =
        denominationDao.upsert(DenominationEntity(value, count))

    /** Inserts a tracked row for [value] at count 0, unless it's already tracked. */
    suspend fun addDenominationIfMissing(value: Int) {
        if (denominationDao.get(value) == null) denominationDao.upsert(DenominationEntity(value, 0))
    }

    // Balances
    val balance = balanceDao.get()

    suspend fun setOnlineAmount(amount: Double) {
        val current = balanceDao.getOnce() ?: BalanceEntity()
        balanceDao.upsert(current.copy(onlineAmount = amount))
    }

    /** Adds [delta] to the online balance. Pass a negative delta to deduct. */
    suspend fun adjustOnline(delta: Double) {
        val current = balanceDao.getOnce() ?: BalanceEntity()
        balanceDao.upsert(current.copy(onlineAmount = current.onlineAmount + delta))
    }

    /** Sets the cash balance directly — used when you type a number, or when you sync it
     * from the note/coin counting tool. */
    suspend fun setCashAmount(amount: Double) {
        val current = balanceDao.getOnce() ?: BalanceEntity()
        balanceDao.upsert(current.copy(cashAmount = amount))
    }

    /** Adds [delta] to the cash balance. Pass a negative delta to deduct. */
    suspend fun adjustCash(delta: Double) {
        val current = balanceDao.getOnce() ?: BalanceEntity()
        balanceDao.upsert(current.copy(cashAmount = current.cashAmount + delta))
    }

    // Lending
    val allLending = lendingDao.getAll()
    suspend fun addLending(entity: LendingEntity) = lendingDao.insert(entity)
    suspend fun updateLending(entity: LendingEntity) = lendingDao.update(entity)
    suspend fun deleteLending(entity: LendingEntity) = lendingDao.delete(entity)

    // Expenses
    val allExpenses = expenseDao.getAll()

    /** Logs a spend and deducts it from the matching balance. */
    suspend fun addExpense(entity: ExpenseEntity) {
        expenseDao.insert(entity)
        if (entity.paymentMethod == PaymentMethod.ONLINE) adjustOnline(-entity.amount)
        else adjustCash(-entity.amount)
    }

    /** Deletes an expense and refunds its amount back to the balance it was deducted from. */
    suspend fun deleteExpense(entity: ExpenseEntity) {
        expenseDao.delete(entity)
        if (entity.paymentMethod == PaymentMethod.ONLINE) adjustOnline(entity.amount)
        else adjustCash(entity.amount)
    }
}
