package com.sujal.expensetracker.ui.lending.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sujal.expensetracker.data.entity.LendingEntity
import com.sujal.expensetracker.data.entity.PaymentMethod
import com.sujal.expensetracker.databinding.ItemLendingBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class LendingAdapter(
    private val onGetPaid: (LendingEntity) -> Unit,
    private val onDelete: (LendingEntity) -> Unit
) : ListAdapter<LendingEntity, LendingAdapter.ViewHolder>(DIFF) {

    private val moneyFormat = DecimalFormat("₹#,##0.##")
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    inner class ViewHolder(val binding: ItemLendingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLendingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            textName.text = item.personName
            val outstanding = (item.amount - item.amountPaid).coerceAtLeast(0.0)
            textAmount.text = if (item.isSettled) moneyFormat.format(item.amount) else moneyFormat.format(outstanding)

            val methodLabel = if (item.paymentMethod == PaymentMethod.ONLINE) "Online" else "Cash"
            val dateStr = dateFormat.format(java.util.Date(item.dateMillis))
            textDate.text = "$dateStr · via $methodLabel"

            if (item.note.isNotBlank()) {
                textNote.visibility = android.view.View.VISIBLE
                textNote.text = item.note
            } else {
                textNote.visibility = android.view.View.GONE
            }

            if (!item.isSettled && item.amountPaid > 0) {
                textProgress.visibility = android.view.View.VISIBLE
                textProgress.text = "${moneyFormat.format(item.amountPaid)} paid of ${moneyFormat.format(item.amount)}"
            } else {
                textProgress.visibility = android.view.View.GONE
            }

            if (item.isSettled) {
                textAmount.paintFlags = textAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                textAmount.setTextColor(root.context.getColor(com.sujal.expensetracker.R.color.settled_green))
                layoutActions.visibility = android.view.View.GONE
            } else {
                textAmount.paintFlags = textAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                textAmount.setTextColor(root.context.getColor(com.sujal.expensetracker.R.color.outstanding_red))
                layoutActions.visibility = android.view.View.VISIBLE
            }

            btnSettle.setOnClickListener { onGetPaid(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LendingEntity>() {
            override fun areItemsTheSame(a: LendingEntity, b: LendingEntity) = a.id == b.id
            override fun areContentsTheSame(a: LendingEntity, b: LendingEntity) = a == b
        }
    }
}
