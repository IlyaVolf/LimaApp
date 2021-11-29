package start.up.tracker.ui.addedittask

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import start.up.tracker.R
import start.up.tracker.databinding.FragmentAddEditTaskBinding
import start.up.tracker.utils.exhaustive
import java.text.SimpleDateFormat

@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private val viewModel: AddEditTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAddEditTaskBinding.bind(view)

        var date = ""

        binding.apply {
            editTextTaskLabel.setText(viewModel.taskName)
            checkBoxImportant.isChecked = viewModel.taskImportance
            checkBoxImportant.jumpDrawablesToCurrentState()

            editTextTaskLabel.addTextChangedListener {
                viewModel.taskName = it.toString()
            }

            checkBoxImportant.setOnCheckedChangeListener { _, isChecked ->
                viewModel.taskImportance = isChecked
            }

            fabSaveTask.setOnClickListener {
                val checkedChip = binding.chipCategoriesGroup.children
                    .toList()
                    .filter { (it as Chip).isChecked }
                    .joinToString { (it as Chip).text }

                viewModel.onSaveClick(checkedChip, date)
            }

            val today = MaterialDatePicker.todayInUtcMilliseconds()
            val materialDatePicker: MaterialDatePicker<Long> = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a date")
                .setSelection(today)
                .build()


            btnDatePicker.setOnClickListener {
                materialDatePicker.show(requireActivity().supportFragmentManager, "DATE_PICKER")
            }

            materialDatePicker.addOnPositiveButtonClickListener {
                val formattedDate = formatToDate(it)
                date = formattedDate
                btnDatePicker.text = formattedDate
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.addEditTaskEvent.collect { event ->
                when(event) {
                    is AddEditTaskViewModel.AddEditTaskEvent.NavigateBackWithResult -> {
                        binding.editTextTaskLabel.clearFocus()
                        setFragmentResult(
                            "add_edit_request",
                            bundleOf("add_edit_result" to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditTaskViewModel.AddEditTaskEvent.ShowInvalidInputMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                }.exhaustive
            }
        }


        /**
         * Dynamically add chips into [ChipGroup]. It checks if the name of category from all categories is in
         * checked. If it is, it means that current task is associated with that category, so
         * we need to isChipChecked = true
         */
        viewModel.combinedCategories.observe(viewLifecycleOwner) {
            val set = it.first
            val subset = it.second

            set.forEach { category ->
                var isChipChecked = false
                if (subset.categoryName == category.categoryName) { isChipChecked = true }
                addCategoryChip(category.categoryName, binding.chipCategoriesGroup, isChipChecked)
            }
        }
    }

    private fun formatToDate(it: Long?): String {
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy")
        return simpleDateFormat.format(it)
    }

    private fun addCategoryChip(chipText: String, chipGroup: ChipGroup, isChipChecked: Boolean) {
        val chip = Chip(context)
        chip.apply {
            text = chipText
            isCheckable = true
            isChipIconVisible = false
            isCheckedIconVisible = true
            isCloseIconVisible = false
            checkedIcon = context?.let { ContextCompat.getDrawable(it, R.drawable.ic_mtrl_chip_checked_black) }
            // TODO(Customise colors later)
            //rippleColor = context?.let { ContextCompat.getColorStateList(it, R.color.mtrl_choice_chip_background_color) }
        }
        chipGroup.addView(chip)
        chip.isChecked = isChipChecked
        chip.isChipIconVisible = isChipChecked
    }
}