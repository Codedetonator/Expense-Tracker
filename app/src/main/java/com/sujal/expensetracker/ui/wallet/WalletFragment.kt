package com.sujal.expensetracker.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujal.expensetracker.R
import com.sujal.expensetracker.databinding.DialogAddDenominationBinding
import com.sujal.expensetracker.databinding.DialogCountCashBinding
import com.sujal.expensetracker.databinding.FragmentWalletBinding
import java.text.DecimalFormat

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by viewModels()
    private val moneyFormat = DecimalFormat("₹#,##0.##")

    private var suppressOnlineWatcher = false
    private var suppressCashWatcher = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.grandTotal.observe(viewLifecycleOwner) {
            binding.textGrandTotal.text = moneyFormat.format(it)
        }

        viewModel.onlineBalance.observe(viewLifecycleOwner) { amount ->
            syncField(binding.editOnlineBalance, amount) { suppressOnlineWatcher = it }
        }
        viewModel.cashBalance.observe(viewLifecycleOwner) { amount ->
            syncField(binding.editCashBalance, amount) { suppressCashWatcher = it }
        }

        binding.editOnlineBalance.addTextChangedListener(watcherFor { amount ->
            if (!suppressOnlineWatcher) viewModel.setOnlineAmount(amount)
        })
        binding.editCashBalance.addTextChangedListener(watcherFor { amount ->
            if (!suppressCashWatcher) viewModel.setCashAmount(amount)
        })

        binding.btnCountCash.setOnClickListener { showCountCashDialog() }
    }

    /** Pushes the stored value into the field, but never while you're typing in it
     * (a late database update used to overwrite what you had just typed). */
    private fun syncField(field: EditText, amount: Double, setSuppress: (Boolean) -> Unit) {
        if (field.hasFocus()) return
        val current = field.text?.toString()?.toDoubleOrNull() ?: -1.0
        if (current != amount) {
            setSuppress(true)
            field.setText(
                when {
                    amount == 0.0 -> ""
                    amount % 1.0 == 0.0 -> amount.toLong().toString()
                    else -> amount.toString()
                }
            )
            setSuppress(false)
        }
    }

    private fun watcherFor(onChanged: (Double) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) {
            onChanged(s?.toString()?.toDoubleOrNull() ?: 0.0)
        }
    }

    // ---------- Count my cash ----------

    private fun showCountCashDialog() {
        // Wait for the saved counts once, then open the dialog with a local draft.
        val live = viewModel.denominationMap
        live.observe(viewLifecycleOwner, object : Observer<Map<Int, Int>> {
            override fun onChanged(value: Map<Int, Int>) {
                live.removeObserver(this)
                openCountCashDialog(value)
            }
        })
    }

    private fun openCountCashDialog(initial: Map<Int, Int>) {
        val d = DialogCountCashBinding.inflate(LayoutInflater.from(requireContext()))
        val draft = initial.toMutableMap()

        fun total() = draft.entries.sumOf { it.key.toDouble() * it.value }
        fun refreshTotal() {
            d.textCountedTotal.text = moneyFormat.format(total())
            d.btnSetCash.text = "${getString(R.string.set_cash_to)} (${moneyFormat.format(total())})"
        }

        val adapter = DenominationAdapter { value, count ->
            draft[value] = count
            refreshTotal()
        }
        adapter.submitRows(draft.toList().sortedByDescending { it.first })
        d.recyclerCountCash.layoutManager = LinearLayoutManager(requireContext())
        d.recyclerCountCash.setHasFixedSize(true)
        d.recyclerCountCash.setItemViewCacheSize(12)
        d.recyclerCountCash.adapter = adapter
        refreshTotal()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.count_my_cash))
            .setView(d.root)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            // Without this the keyboard never opens: Android marks dialogs "no input" when
            // no text field exists at creation time, and our list is empty at that moment.
            dialog.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            )
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        // Counts are remembered when you close; the cash balance only changes on Update.
        dialog.setOnDismissListener { viewModel.saveCounts(draft) }

        d.btnAddDenomination.setOnClickListener {
            showAddDenominationDialog { value ->
                if (draft.containsKey(value)) {
                    Toast.makeText(requireContext(), "₹$value is already in the list", Toast.LENGTH_SHORT).show()
                } else {
                    draft[value] = 0
                    val index = adapter.addRow(value)
                    if (index >= 0) d.recyclerCountCash.scrollToPosition(index)
                }
            }
        }

        d.btnSetCash.setOnClickListener {
            val newCash = total()
            viewModel.saveCounts(draft)
            viewModel.setCashAmount(newCash)
            Toast.makeText(requireContext(), "Cash updated to ${moneyFormat.format(newCash)}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddDenominationDialog(onValue: (Int) -> Unit) {
        val d = DialogAddDenominationBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_note_coin))
            .setView(d.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            d.editDenominationValue.requestFocus()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = d.editDenominationValue.text.toString().toIntOrNull() ?: 0
                if (value <= 0) {
                    Toast.makeText(requireContext(), "Enter a valid value", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onValue(value)
                viewModel.addCustomDenomination(value)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
