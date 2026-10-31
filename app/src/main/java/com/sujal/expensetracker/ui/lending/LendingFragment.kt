package com.sujal.expensetracker.ui.lending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujal.expensetracker.data.entity.LendingEntity
import com.sujal.expensetracker.data.entity.PaymentMethod
import com.sujal.expensetracker.databinding.DialogAddLendingBinding
import com.sujal.expensetracker.databinding.DialogRepayAmountBinding
import com.sujal.expensetracker.databinding.FragmentLendingBinding
import com.sujal.expensetracker.ui.lending.adapter.LendingAdapter
import java.text.DecimalFormat

class LendingFragment : Fragment() {

    private var _binding: FragmentLendingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LendingViewModel by viewModels()
    private lateinit var adapter: LendingAdapter
    private val moneyFormat = DecimalFormat("₹#,##0.##")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLendingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LendingAdapter(
            onGetPaid = { showRepayDialog(it) },
            onDelete = { viewModel.delete(it) }
        )
        binding.recyclerLending.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLending.adapter = adapter

        viewModel.allLending.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalOutstanding.observe(viewLifecycleOwner) {
            binding.textTotalLent.text = moneyFormat.format(it)
        }

        binding.fabAdd.setOnClickListener { showAddDialog() }
    }

    private fun toast(message: String) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    private fun keepOpenUntilValid(dialog: AlertDialog, onConfirm: () -> Boolean) {
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (onConfirm()) dialog.dismiss()
            }
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddLendingBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add lending")
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        keepOpenUntilValid(dialog) {
            val name = dialogBinding.editName.text.toString().trim()
            val amount = dialogBinding.editAmount.text.toString().toDoubleOrNull() ?: 0.0
            val note = dialogBinding.editNote.text.toString().trim()
            val method = if (dialogBinding.radioMethod.checkedRadioButtonId == dialogBinding.radioOnline.id)
                PaymentMethod.ONLINE else PaymentMethod.CASH

            if (name.isEmpty()) {
                toast("Enter a name")
                return@keepOpenUntilValid false
            }
            if (amount <= 0) {
                toast("Enter an amount")
                return@keepOpenUntilValid false
            }
            viewModel.add(name, amount, note, method)
            true
        }

        dialog.show()
    }

    private fun showRepayDialog(entity: LendingEntity) {
        val outstanding = (entity.amount - entity.amountPaid).coerceAtLeast(0.0)
        val dialogBinding = DialogRepayAmountBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.textOutstanding.text = "Outstanding: ${moneyFormat.format(outstanding)}"
        dialogBinding.editRepayAmount.setText(if (outstanding > 0) outstanding.toString() else "")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Get paid")
            .setView(dialogBinding.root)
            .setPositiveButton("Done", null)
            .setNegativeButton("Cancel", null)
            .create()

        keepOpenUntilValid(dialog) {
            val amount = dialogBinding.editRepayAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                toast("Enter an amount")
                return@keepOpenUntilValid false
            }
            val method = if (dialogBinding.radioRepayMethod.checkedRadioButtonId == dialogBinding.radioRepayOnline.id)
                PaymentMethod.ONLINE else PaymentMethod.CASH
            viewModel.repay(entity, amount, method)
            true
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
