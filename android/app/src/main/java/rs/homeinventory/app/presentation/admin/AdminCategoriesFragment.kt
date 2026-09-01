package rs.homeinventory.app.presentation.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.DialogAdminCategoryFormBinding
import rs.homeinventory.app.databinding.FragmentAdminCategoriesBinding
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState

// SCR-13 — dodavanje/preimenovanje/brisanje globalnih kategorija (tiket 25). Isti obrazac dijaloga
// i potvrde brisanja kao LocationsFragment (tiket 17).
@AndroidEntryPoint
class AdminCategoriesFragment : Fragment(R.layout.fragment_admin_categories) {

    private var _binding: FragmentAdminCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminCategoriesViewModel by viewModels()

    private val adapter = AdminCategoriesAdapter(::showEditDialog, ::onDeleteClicked)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminCategoriesBinding.bind(view)

        binding.recyclerAdminCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAdminCategories.adapter = adapter
        binding.recyclerAdminCategories.setHasFixedSize(true)

        binding.fabAddCategory.setOnClickListener { showAddDialog() }
        binding.buttonEmptyAction.setOnClickListener { showAddDialog() }
        binding.buttonErrorRetry.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.deleteState.collect(::renderDeleteState) }
            }
        }
    }

    private fun render(state: UiState<List<AdminCategoryUi>>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.recyclerAdminCategories.isVisible = state is UiState.Success
        binding.groupEmpty.isVisible = state is UiState.Empty
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> adapter.submitList(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    private fun renderDeleteState(state: Resource<Unit>?) {
        when (state) {
            is Resource.Success -> {
                Snackbar.make(binding.root, R.string.admin_categories_delete_confirmation, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeDeleteState()
            }
            is Resource.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.consumeDeleteState()
            }
            else -> Unit
        }
    }

    private fun showAddDialog() = showCategoryDialog(existing = null)

    private fun showEditDialog(category: AdminCategoryUi) = showCategoryDialog(existing = category)

    // Isti dijalog sluzi za dodavanje i preimenovanje; existing == null znaci dodavanje.
    private fun showCategoryDialog(existing: AdminCategoryUi?) {
        viewModel.resetForm()

        val dialogBinding = DialogAdminCategoryFormBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.editCategoryName.setText(existing?.name.orEmpty())
        dialogBinding.editCategoryName.doOnTextChanged { _, _, _, _ -> dialogBinding.layoutCategoryName.error = null }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.admin_categories_dialog_add_title else R.string.admin_categories_dialog_edit_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.admin_categories_dialog_save, null)
            .setNegativeButton(R.string.admin_categories_dialog_cancel, null)
            .create()

        // Sopstvena kolekcija vezana za zivotni vek dijaloga — otkazuje se kad se dijalog zatvori,
        // jer polja iz dialogBinding-a posle toga vise ne postoje.
        var collectJob: Job? = null
        dialog.setOnShowListener {
            collectJob = viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.fieldError.collect { error ->
                            dialogBinding.layoutCategoryName.error = error?.let(::getString)
                        }
                    }
                    launch {
                        viewModel.formState.collect { state ->
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = state !is Resource.Loading
                            when (state) {
                                is Resource.Success -> dialog.dismiss()
                                is Resource.Error -> {
                                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                viewModel.save(existing?.id, dialogBinding.editCategoryName.text?.toString().orEmpty())
            }
        }
        dialog.setOnDismissListener { collectJob?.cancel() }

        dialog.show()
    }

    // Kategorija u upotrebi se ne moze obrisati; broj predmeta je vec prikazan na ekranu.
    private fun onDeleteClicked(category: AdminCategoryUi) {
        if (category.itemCount > 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_categories_delete_blocked_title)
                .setMessage(
                    getString(R.string.admin_categories_delete_blocked_message, category.name, category.itemCount)
                )
                .setPositiveButton(R.string.admin_categories_delete_blocked_confirm, null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.admin_categories_delete_dialog_title)
            .setMessage(getString(R.string.admin_categories_delete_dialog_message, category.name))
            .setPositiveButton(R.string.admin_categories_delete_dialog_confirm) { _, _ -> viewModel.delete(category.id) }
            .setNegativeButton(R.string.admin_categories_delete_dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerAdminCategories.adapter = null
        _binding = null
    }
}
