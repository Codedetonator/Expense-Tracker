package com.sujal.expensetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.sujal.expensetracker.databinding.ActivityMainBinding
import com.sujal.expensetracker.ui.expenses.ExpensesFragment
import com.sujal.expensetracker.ui.lending.LendingFragment
import com.sujal.expensetracker.ui.wallet.WalletFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(WalletFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_wallet -> {
                    showFragment(WalletFragment())
                    true
                }
                R.id.nav_expenses -> {
                    showFragment(ExpensesFragment())
                    true
                }
                R.id.nav_lending -> {
                    showFragment(LendingFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
