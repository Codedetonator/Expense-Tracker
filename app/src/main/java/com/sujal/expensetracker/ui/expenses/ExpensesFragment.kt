package com.sujal.expensetracker.ui.expenses

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujal.expensetracker.R
import com.sujal.expensetracker.data.entity.ExpenseEntity
import com.sujal.expensetracker.data.entity.PaymentMethod
import com.sujal.expensetracker.databinding.FragmentExpensesBinding
import com.sujal.expensetracker.databinding.ItemBarRowBinding
import com.sujal.expensetracker.ui.expenses.adapter.ExpenseAdapter
import com.sujal.expensetracker.ui.expenses.adapter.buildExpenseListRows
import com.sujal.expensetracker.util.EXPENSE_CATEGORIES
import com.sujal.expensetracker.util.emojiForCategory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpensesViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter

    private val moneyFormat = DecimalFormat("₹#,##0.##")
    private val fullDateFormat = SimpleDateFormat("EEEE d MMMM, yyyy", Locale.getDefault())
    private val fieldDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val shortDayFormat = SimpleDateFormat("EEE d", Locale.getDefault())

    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var selectedCategoryFilter: String? = null // null = All
    private var latestExpenses: List<ExpenseEntity> = emptyList()

    private enum class Tab { ADD, LIST, CHART }
    private var currentTab = Tab.ADD

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textHeaderDate.text = fullDateFormat.format(Date())

        setUpAddForm()
        setUpTabs()
        setUpCategoryChips()

        adapter = ExpenseAdapter(onDelete = { viewModel.delete(it) })
        binding.recyclerExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExpenses.adapter = adapter

        viewModel.allExpenses.observe(viewLifecycleOwner) { list ->
            latestExpenses = list
            binding.textStatEntries.text = list.size.toString()
            applyFilterAndRender()
            renderCategoryChart(list)
            renderLast7DaysChart(list)
        }

        viewModel.totalToday.observe(viewLifecycleOwner) {
            binding.textStatToday.text = moneyFormat.format(it)
        }
        viewModel.totalThisMonth.observe(viewLifecycleOwner) {
            binding.textStatMonth.text = moneyFormat.format(it)
        }
    }

    // ---------- Tabs ----------

    private fun setUpTabs() {
        binding.btnTabAdd.setOnClickListener { selectTab(Tab.ADD) }
        binding.btnTabList.setOnClickListener { selectTab(Tab.LIST) }
        binding.btnTabChart.setOnClickListener { selectTab(Tab.CHART) }
        selectTab(Tab.ADD)
    }

    private fun selectTab(tab: Tab) {
        currentTab = tab
        binding.sectionAdd.visibility = if (tab == Tab.ADD) View.VISIBLE else View.GONE
        binding.sectionList.visibility = if (tab == Tab.LIST) View.VISIBLE else View.GONE
        binding.sectionChart.visibility = if (tab == Tab.CHART) View.VISIBLE else View.GONE

        val activeColor = requireContext().getColor(R.color.primary)
        val inactiveColor = requireContext().getColor(R.color.surface)
        val activeText = requireContext().getColor(android.R.color.white)
        val inactiveText = requireContext().getColor(R.color.text_primary)

        val buttons = listOf(binding.btnTabAdd, binding.btnTabList, binding.btnTabChart)
        val tabs = listOf(Tab.ADD, Tab.LIST, Tab.CHART)
        buttons.forEachIndexed { i, button ->
            val isActive = tabs[i] == tab
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isActive) activeColor else inactiveColor)
            button.setTextColor(if (isActive) activeText else inactiveText)
        }
    }

    // ---------- Add form ----------

    private fun setUpAddForm() {
        binding.textDatePicker.text = fieldDateFormat.format(Date(selectedDateMillis))
        binding.textDatePicker.setOnClickListener { showDatePicker() }

        val categoryNames = EXPENSE_CATEGORIES.map { it.first }
        val spinnerAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, categoryNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view.findViewById<TextView>(android.R.id.text1)).text =
                    "${emojiForCategory(categoryNames[position])} ${categoryNames[position]}"
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view.findViewById<TextView>(android.R.id.text1)).text =
                    "${emojiForCategory(categoryNames[position])} ${categoryNames[position]}"
                return view
            }
        }
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter

        binding.btnAddExpense.setOnClickListener {
            val description = binding.editDescription.text.toString().trim()
            val amount = binding.editAmount.text.toString().toDoubleOrNull() ?: 0.0
            val category = binding.spinnerCategory.selectedItem?.toString() ?: "Other"
            val note = binding.editNote.text.toString().trim()
            val method = if (binding.radioMethod.checkedRadioButtonId == binding.radioOnline.id)
                PaymentMethod.ONLINE else PaymentMethod.CASH

            if (description.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.add(description, amount, category, note, method, selectedDateMillis)

            binding.editDescription.text.clear()
            binding.editAmount.text.clear()
            binding.editNote.text.clear()
            selectTab(Tab.LIST)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                selectedDateMillis = cal.timeInMillis
                binding.textDatePicker.text = fieldDateFormat.format(Date(selectedDateMillis))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ---------- List + category chips ----------

    private fun setUpCategoryChips() {
        binding.chipContainer.removeAllViews()
        addChip(getString(R.string.category_all), null)
        EXPENSE_CATEGORIES.forEach { (name, emoji) -> addChip("$emoji $name", name) }
    }

    private fun addChip(label: String, categoryValue: String?) {
        val chip = TextView(requireContext()).apply {
            text = label
            setPadding(28, 16, 28, 16)
            textSize = 13f
            setTextColor(requireContext().getColor(
                if (categoryValue == selectedCategoryFilter) android.R.color.white else R.color.text_primary
            ))
            setBackgroundResource(
                if (categoryValue == selectedCategoryFilter) R.drawable.chip_selected_bg else R.drawable.chip_unselected_bg
            )
            layoutParams = chipLayoutParams()
            setOnClickListener {
                selectedCategoryFilter = categoryValue
                setUpCategoryChips()
                applyFilterAndRender()
            }
        }
        binding.chipContainer.addView(chip)
    }

    private fun chipLayoutParams() = android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { rightMargin = 8.dp; topMargin = 4; bottomMargin = 4 }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    private fun applyFilterAndRender() {
        val filtered = selectedCategoryFilter?.let { cat -> latestExpenses.filter { it.category == cat } } ?: latestExpenses
        adapter.submitList(buildExpenseListRows(filtered))
        binding.textEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------- Charts ----------

    private fun renderCategoryChart(list: List<ExpenseEntity>) {
        binding.containerCategoryBars.removeAllViews()
        val totals = list.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
            .filterValues { it > 0 }
            .toList().sortedByDescending { it.second }

        if (totals.isEmpty()) {
            addEmptyChartMessage(binding.containerCategoryBars)
            return
        }
        val max = totals.maxOf { it.second }
        totals.forEach { (category, amount) ->
            val row = ItemBarRowBinding.inflate(LayoutInflater.from(requireContext()), binding.containerCategoryBars, false)
            row.textBarLabel.text = "${emojiForCategory(category)} $category"
            row.textBarAmount.text = moneyFormat.format(amount)
            row.progressBar.max = 1000
            row.progressBar.progress = ((amount / max) * 1000).toInt()
            binding.containerCategoryBars.addView(row.root)
        }
    }

    private fun renderLast7DaysChart(list: List<ExpenseEntity>) {
        binding.containerDayBars.removeAllViews()
        val days = (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -offset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val end = start + 24L * 60 * 60 * 1000
            val total = list.filter { it.dateMillis in start until end }.sumOf { it.amount }
            Triple(shortDayFormat.format(Date(start)), total, offset == 0)
        }
        val max = days.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
        days.forEach { (label, amount, isToday) ->
            val row = ItemBarRowBinding.inflate(LayoutInflater.from(requireContext()), binding.containerDayBars, false)
            row.textBarLabel.text = label
            row.textBarAmount.text = moneyFormat.format(amount)
            row.progressBar.max = 1000
            row.progressBar.progress = if (amount > 0) ((amount / max) * 1000).toInt() else 20
            val tint = if (isToday && amount > 0) R.color.primary else R.color.text_secondary
            row.textBarLabel.setTextColor(requireContext().getColor(tint))
            if (isToday) row.textBarLabel.setTypeface(row.textBarLabel.typeface, android.graphics.Typeface.BOLD)
            binding.containerDayBars.addView(row.root)
        }
    }

    private fun addEmptyChartMessage(container: android.widget.LinearLayout) {
        val tv = TextView(requireContext()).apply {
            text = getString(R.string.no_expenses_yet)
            setTextColor(requireContext().getColor(R.color.text_secondary))
            textSize = 13f
        }
        container.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
