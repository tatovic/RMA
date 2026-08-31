package rs.homeinventory.app.presentation.itemdetails

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentItemDetailsBinding
import rs.homeinventory.app.databinding.ItemDetailFieldBinding
import rs.homeinventory.app.util.EXTRA_ITEM_DELETED_ID
import rs.homeinventory.app.util.RESULT_ITEM_SAVED
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import rs.homeinventory.app.util.photoFile

// SCR-07 — detalji predmeta i brisanje (tiket 16). BR-007: ekran prima samo itemId i sam ucitava iz Room-a.
@AndroidEntryPoint
class ItemDetailsFragment : Fragment(R.layout.fragment_item_details) {

    private var _binding: FragmentItemDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentItemDetailsBinding.bind(view)

        binding.buttonEdit.setOnClickListener {
            findNavController().navigate(
                R.id.action_itemDetailsFragment_to_addEditItemFragment,
                bundleOf("itemId" to viewModel.itemId)
            )
        }
        binding.buttonDelete.setOnClickListener { confirmDelete() }

        // Forma za izmenu vraca rezultat ovde (tiket 15) — kratka potvrda korisniku.
        setFragmentResultListener(RESULT_ITEM_SAVED) { _, _ ->
            Snackbar.make(binding.root, R.string.inventory_item_saved_confirmation, Snackbar.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.deleteState.collect(::renderDeleteState) }
            }
        }
    }

    private fun render(state: UiState<ItemDetailsUi>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.scrollContent.isVisible = state is UiState.Success
        binding.containerActions.isVisible = state is UiState.Success
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> renderItem(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    private fun renderItem(item: ItemDetailsUi) {
        binding.textName.text = item.name
        binding.textCategoryLocation.text =
            getString(R.string.inventory_item_subtitle, item.categoryName, item.locationName)

        // Fotografija se prikazuje samo ako postoji.
        binding.imagePhoto.isVisible = item.imagePath != null
        if (item.imagePath != null) {
            Glide.with(binding.imagePhoto)
                .load(photoFile(requireContext(), item.imagePath))
                .centerCrop()
                .into(binding.imagePhoto)
        } else {
            Glide.with(binding.imagePhoto).clear(binding.imagePhoto)
        }

        bindField(binding.rowDescription, R.string.additem_label_description, item.description)
        bindField(binding.rowManufacturer, R.string.additem_label_manufacturer, item.manufacturer)
        bindField(binding.rowModel, R.string.additem_label_model, item.model)
        bindField(binding.rowSerialNumber, R.string.additem_label_serial_number, item.serialNumber)
        bindField(binding.rowQuantity, R.string.additem_label_quantity, item.quantity.toString())
        bindField(binding.rowPurchasePrice, R.string.additem_label_purchase_price, item.purchasePriceFormatted)
        bindField(binding.rowEstimatedValue, R.string.additem_label_estimated_value, item.estimatedValueFormatted)
        bindField(binding.rowPurchaseDate, R.string.additem_label_purchase_date, item.purchaseDateFormatted)
        bindField(
            binding.rowWarrantyDate,
            R.string.additem_label_warranty_expiration_date,
            item.warrantyExpirationDateFormatted
        )
        bindField(binding.rowSeller, R.string.additem_label_seller, item.seller)
        bindField(binding.rowNotes, R.string.additem_label_notes, item.notes)
    }

    // Prazno polje se ne prikazuje kao prazan red.
    private fun bindField(row: ItemDetailFieldBinding, @StringRes labelRes: Int, value: String?) {
        row.root.isVisible = value != null
        if (value != null) {
            row.textFieldLabel.setText(labelRes)
            row.textFieldValue.text = value
        }
    }

    // BR-008 — destruktivna akcija trazi potvrdu sa nazivom predmeta u tekstu.
    private fun confirmDelete() {
        val name = (viewModel.state.value as? UiState.Success)?.data?.name ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.itemdetails_delete_dialog_title)
            .setMessage(getString(R.string.itemdetails_delete_dialog_message, name))
            .setPositiveButton(R.string.itemdetails_delete_dialog_confirm) { _, _ -> viewModel.delete() }
            .setNegativeButton(R.string.itemdetails_delete_dialog_cancel, null)
            .show()
    }

    private fun renderDeleteState(state: Resource<Unit>?) {
        binding.buttonDelete.isEnabled = state !is Resource.Loading
        binding.buttonEdit.isEnabled = state !is Resource.Loading

        // Ekran se zatvara odmah po brisanju; opoziv u roku od pet sekundi (FR-027) se nudi
        // na listi posle povratka, jer je ItemDetailsActivity zaseban ekran (tiket 14).
        if (state is Resource.Success) {
            requireActivity().apply {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_ITEM_DELETED_ID, viewModel.itemId))
                finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
