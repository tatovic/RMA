package rs.homeinventory.app.presentation.locations

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
import rs.homeinventory.app.databinding.DialogLocationFormBinding
import rs.homeinventory.app.databinding.FragmentLocationsBinding
import rs.homeinventory.app.util.LocationValidator
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState

// SCR-10 — upravljanje lokacijama (tiket 17). Dostupno iz profila (SCR-09).
@AndroidEntryPoint
class LocationsFragment : Fragment(R.layout.fragment_locations) {

    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationsViewModel by viewModels()

    private val adapter = LocationsAdapter(::showEditDialog, ::onDeleteClicked)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocationsBinding.bind(view)

        binding.recyclerLocations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLocations.adapter = adapter
        binding.recyclerLocations.setHasFixedSize(true)

        binding.fabAddLocation.setOnClickListener { showAddDialog() }
        binding.buttonEmptyAction.setOnClickListener { showAddDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.deleteState.collect(::renderDeleteState) }
            }
        }
    }

    private fun render(state: UiState<List<LocationUi>>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.recyclerLocations.isVisible = state is UiState.Success
        binding.groupEmpty.isVisible = state is UiState.Empty

        if (state is UiState.Success) adapter.submitList(state.data)
    }

    private fun renderDeleteState(state: Resource<Unit>?) {
        when (state) {
            is Resource.Success -> {
                Snackbar.make(binding.root, R.string.locations_delete_confirmation, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeDeleteState()
            }
            is Resource.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.consumeDeleteState()
            }
            else -> Unit
        }
    }

    private fun showAddDialog() = showLocationDialog(existing = null)

    private fun showEditDialog(location: LocationUi) = showLocationDialog(existing = location)

    // Isti dijalog sluzi za dodavanje i izmenu; existing == null znaci dodavanje.
    private fun showLocationDialog(existing: LocationUi?) {
        viewModel.resetForm()

        val dialogBinding = DialogLocationFormBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.editLocationName.setText(existing?.name.orEmpty())
        dialogBinding.editLocationDescription.setText(existing?.description.orEmpty())
        dialogBinding.editLocationName.doOnTextChanged { _, _, _, _ -> dialogBinding.layoutLocationName.error = null }
        dialogBinding.editLocationDescription.doOnTextChanged { _, _, _, _ ->
            dialogBinding.layoutLocationDescription.error = null
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.locations_dialog_add_title else R.string.locations_dialog_edit_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.locations_dialog_save, null)
            .setNegativeButton(R.string.locations_dialog_cancel, null)
            .create()

        // Sopstvena kolekcija vezana za zivotni vek dijaloga — otkazuje se kad se dijalog zatvori,
        // jer polja iz dialogBinding-a posle toga vise ne postoje.
        var collectJob: Job? = null
        dialog.setOnShowListener {
            collectJob = viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.fieldErrors.collect { errors ->
                            dialogBinding.layoutLocationName.error = errors.name?.let(::getString)
                            dialogBinding.layoutLocationDescription.error = errors.description?.let(::getString)
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
                val input = LocationValidator.Input(
                    name = dialogBinding.editLocationName.text?.toString().orEmpty(),
                    description = dialogBinding.editLocationDescription.text?.toString().orEmpty()
                )
                viewModel.save(existing?.id, input)
            }
        }
        dialog.setOnDismissListener { collectJob?.cancel() }

        dialog.show()
    }

    // BR-014 — lokacija sa predmetima se ne moze obrisati; broj predmeta je vec prikazan na ekranu.
    private fun onDeleteClicked(location: LocationUi) {
        if (location.itemCount > 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.locations_delete_blocked_title)
                .setMessage(
                    getString(R.string.locations_delete_blocked_message, location.name, location.itemCount)
                )
                .setPositiveButton(R.string.locations_delete_blocked_confirm, null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.locations_delete_dialog_title)
            .setMessage(getString(R.string.locations_delete_dialog_message, location.name))
            .setPositiveButton(R.string.locations_delete_dialog_confirm) { _, _ -> viewModel.delete(location.id) }
            .setNegativeButton(R.string.locations_delete_dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerLocations.adapter = null
        _binding = null
    }
}
