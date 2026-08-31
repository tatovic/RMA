package rs.homeinventory.app.presentation.additem

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.databinding.FragmentAddEditItemBinding
import rs.homeinventory.app.util.ItemValidator
import rs.homeinventory.app.util.RESULT_ITEM_SAVED
import rs.homeinventory.app.util.RESULT_ITEM_SAVED_ID
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SUPPORTED_CURRENCIES
import rs.homeinventory.app.util.photoFile
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// SCR-06 — ista forma za dodavanje i izmenu predmeta (tiket 15). itemId (Safe Args, null = dodavanje)
// se cita direktno u ViewModel-u kroz SavedStateHandle.
@AndroidEntryPoint
class AddEditItemFragment : Fragment(R.layout.fragment_add_edit_item) {

    // K-05.
    private var _binding: FragmentAddEditItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditItemViewModel by viewModels()

    private val dateDisplayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

    private var categoriesSnapshot: List<CategoryEntity> = emptyList()
    private var locationsSnapshot: List<LocationEntity> = emptyList()

    private var selectedCategoryId: String? = null
    private var selectedLocationId: String? = null
    private var selectedPurchaseDate: LocalDate? = null
    private var selectedWarrantyDate: LocalDate? = null

    // Fotografija koju predmet vec ima (edit rezim); null u rezimu dodavanja ili ako predmet nema sliku.
    private var existingImagePath: String? = null

    // Odrediste snimka kamere u toku (FR-081) — TakePicture vraca samo uspeh/neuspeh, ne i sam Uri.
    private var pendingCameraUri: Uri? = null

    // Snimak forme odmah posle popunjavanja — poredi se sa trenutnim stanjem pri napustanju ekrana.
    private var initialSnapshot: FormSnapshot? = null

    private val galleryPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    private val cameraCapture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) viewModel.onPhotoPicked(uri)
    }

    // FR: odbijena dozvola za kameru se obradjuje porukom, bez rusenja aplikacije.
    private val cameraPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else showCameraPermissionDenied()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditItemBinding.bind(view)

        binding.textTitle.setText(
            if (viewModel.isEditMode) R.string.additem_title_edit else R.string.additem_title_add
        )

        binding.editCurrency.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, SUPPORTED_CURRENCIES)
        )

        binding.textMoreToggle.setOnClickListener {
            binding.containerMore.isVisible = !binding.containerMore.isVisible
        }

        binding.imagePhotoPreview.setOnClickListener { showPhotoChooser() }
        binding.buttonChangePhoto.setOnClickListener { showPhotoChooser() }

        binding.editPurchaseDate.setOnClickListener { pickDate(isWarranty = false) }
        binding.editWarrantyDate.setOnClickListener { pickDate(isWarranty = true) }

        binding.editCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categoriesSnapshot.getOrNull(position)?.id
            binding.layoutCategory.error = null
        }
        binding.editLocation.setOnItemClickListener { _, _, position, _ ->
            selectedLocationId = locationsSnapshot.getOrNull(position)?.id
            binding.layoutLocation.error = null
        }

        clearErrorOnEdit(binding.editName, binding.layoutName)
        clearErrorOnEdit(binding.editQuantity, binding.layoutQuantity)
        clearErrorOnEdit(binding.editPurchasePrice, binding.layoutPurchasePrice)
        clearErrorOnEdit(binding.editEstimatedValue, binding.layoutEstimatedValue)
        clearErrorOnEdit(binding.editSerialNumber, binding.layoutSerialNumber)
        clearErrorOnEdit(binding.editDescription, binding.layoutDescription)
        clearErrorOnEdit(binding.editNotes, binding.layoutNotes)
        clearErrorOnEdit(binding.editManufacturer, binding.layoutManufacturer)
        clearErrorOnEdit(binding.editModel, binding.layoutModel)
        clearErrorOnEdit(binding.editSeller, binding.layoutSeller)

        binding.buttonSave.setOnClickListener { onSaveClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.categories.collect(::renderCategories) }
                launch { viewModel.locations.collect(::renderLocations) }
                launch { viewModel.fieldErrors.collect(::renderFieldErrors) }
                launch { viewModel.saveState.collect(::renderSaveState) }
                launch { viewModel.pendingImagePath.collect(::renderPhotoPreview) }
            }
        }

        // Jednokratno popunjavanje forme (edit rezim) — namerno van repeatOnLifecycle da se ne
        // ponovi i ne prepise korisnikov unos posle pauze/nastavka aplikacije.
        viewLifecycleOwner.lifecycleScope.launch {
            val initial = viewModel.loadInitialData()
            populateForm(initial)
            initialSnapshot = currentSnapshot()
            binding.progressLoading.isVisible = false
            binding.groupForm.isVisible = true
        }

        // Napustanje forme sa nesacuvanim izmenama trazi potvrdu.
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { handleBackPressed() }
    }

    private fun clearErrorOnEdit(edit: TextInputEditText, layout: TextInputLayout) {
        edit.doOnTextChanged { _, _, _, _ -> layout.error = null }
    }

    private fun populateForm(initial: AddEditItemViewModel.InitialData) {
        val item = initial.existingItem
        binding.editName.setText(item?.name.orEmpty())
        binding.editDescription.setText(item?.description.orEmpty())
        binding.editManufacturer.setText(item?.manufacturer.orEmpty())
        binding.editModel.setText(item?.model.orEmpty())
        binding.editSerialNumber.setText(item?.serialNumber.orEmpty())
        binding.editQuantity.setText((item?.quantity ?: 1).toString())
        binding.editPurchasePrice.setText(item?.purchasePrice?.let(::formatMinorForInput).orEmpty())
        binding.editEstimatedValue.setText(item?.estimatedValue?.let(::formatMinorForInput).orEmpty())
        binding.editSeller.setText(item?.seller.orEmpty())
        binding.editNotes.setText(item?.notes.orEmpty())

        val currency = item?.currency ?: initial.defaultCurrency
        binding.editCurrency.setText(currency, false)

        selectedPurchaseDate = item?.purchaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        selectedWarrantyDate = item?.warrantyExpirationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        binding.editPurchaseDate.setText(selectedPurchaseDate?.format(dateDisplayFormatter).orEmpty())
        binding.editWarrantyDate.setText(selectedWarrantyDate?.format(dateDisplayFormatter).orEmpty())

        selectedCategoryId = item?.categoryId
        selectedLocationId = item?.locationId
        applyCategorySelection()
        applyLocationSelection()

        existingImagePath = item?.imagePath
        renderPhotoPreview(viewModel.pendingImagePath.value)

        // Izmena vec ima popunjene dodatne podatke — sekcija se odmah otvara da korisnik sve vidi.
        if (hasAdditionalData(item)) binding.containerMore.isVisible = true
    }

    private fun hasAdditionalData(item: InventoryItemEntity?): Boolean {
        item ?: return false
        return item.description != null || item.manufacturer != null || item.model != null ||
            item.serialNumber != null || item.quantity != 1 || item.purchasePrice != null ||
            item.estimatedValue != null || item.purchaseDate != null ||
            item.warrantyExpirationDate != null || item.seller != null || item.notes != null
    }

    private fun formatMinorForInput(minor: Long): String = BigDecimal.valueOf(minor).movePointLeft(2).toPlainString()

    private fun renderCategories(categories: List<CategoryEntity>) {
        categoriesSnapshot = categories
        binding.editCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories.map { it.name })
        )
        applyCategorySelection()
    }

    private fun renderLocations(locations: List<LocationEntity>) {
        locationsSnapshot = locations
        binding.editLocation.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations.map { it.name })
        )
        applyLocationSelection()
    }

    private fun applyCategorySelection() {
        val name = categoriesSnapshot.find { it.id == selectedCategoryId }?.name ?: return
        binding.editCategory.setText(name, false)
    }

    private fun applyLocationSelection() {
        val name = locationsSnapshot.find { it.id == selectedLocationId }?.name ?: return
        binding.editLocation.setText(name, false)
    }

    // FR-081 do FR-083 — korisnik bira izmedju kamere i galerije za fotografiju predmeta.
    private fun showPhotoChooser() {
        val options = arrayOf(
            getString(R.string.additem_photo_chooser_camera),
            getString(R.string.additem_photo_chooser_gallery)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.additem_photo_chooser_title)
            .setItems(options) { _, which -> if (which == 0) requestCameraCapture() else openGallery() }
            .show()
    }

    // Sistemski birac fotografija (Photo Picker) ne trazi dozvolu za pristup galeriji.
    private fun openGallery() {
        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Dozvola za kameru se trazi na licu mesta, samo kad korisnik zapravo pokusa da slika.
    private fun requestCameraCapture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val uri = viewModel.createCaptureUri()
        pendingCameraUri = uri
        cameraCapture.launch(uri)
    }

    private fun showCameraPermissionDenied() {
        Snackbar.make(binding.root, R.string.additem_photo_camera_permission_denied, Snackbar.LENGTH_LONG).show()
    }

    // Novoodabrana fotografija ima prednost nad onom koju predmet vec ima (edit rezim).
    private fun renderPhotoPreview(pendingImagePath: String?) {
        val fileName = pendingImagePath ?: existingImagePath
        if (fileName != null) {
            Glide.with(binding.imagePhotoPreview)
                .load(photoFile(requireContext(), fileName))
                .placeholder(R.drawable.ic_add)
                .centerCrop()
                .into(binding.imagePhotoPreview)
        } else {
            Glide.with(binding.imagePhotoPreview).clear(binding.imagePhotoPreview)
            binding.imagePhotoPreview.setImageResource(R.drawable.ic_add)
        }
    }

    // Datumi se biraju iskljucivo kroz MaterialDatePicker, nikada kucanjem.
    private fun pickDate(isWarranty: Boolean) {
        val current = if (isWarranty) selectedWarrantyDate else selectedPurchaseDate
        val titleRes = if (isWarranty) {
            R.string.additem_label_warranty_expiration_date
        } else {
            R.string.additem_label_purchase_date
        }
        val builder = MaterialDatePicker.Builder.datePicker().setTitleText(titleRes)
        current?.let { builder.setSelection(it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()) }

        builder.build().apply {
            addOnPositiveButtonClickListener { selection ->
                val date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()
                if (isWarranty) {
                    selectedWarrantyDate = date
                    binding.editWarrantyDate.setText(date.format(dateDisplayFormatter))
                    binding.layoutWarrantyDate.error = null
                } else {
                    selectedPurchaseDate = date
                    binding.editPurchaseDate.setText(date.format(dateDisplayFormatter))
                    binding.layoutPurchaseDate.error = null
                }
            }
        }.show(childFragmentManager, if (isWarranty) "warrantyDatePicker" else "purchaseDatePicker")
    }

    private fun onSaveClicked() {
        val input = ItemValidator.Input(
            name = binding.editName.text?.toString().orEmpty(),
            categoryId = selectedCategoryId,
            locationId = selectedLocationId,
            description = binding.editDescription.text?.toString().orEmpty(),
            manufacturer = binding.editManufacturer.text?.toString().orEmpty(),
            model = binding.editModel.text?.toString().orEmpty(),
            serialNumber = binding.editSerialNumber.text?.toString().orEmpty(),
            quantityRaw = binding.editQuantity.text?.toString().orEmpty(),
            purchasePriceRaw = binding.editPurchasePrice.text?.toString().orEmpty(),
            estimatedValueRaw = binding.editEstimatedValue.text?.toString().orEmpty(),
            currency = binding.editCurrency.text?.toString().orEmpty(),
            purchaseDate = selectedPurchaseDate,
            warrantyExpirationDate = selectedWarrantyDate,
            seller = binding.editSeller.text?.toString().orEmpty(),
            notes = binding.editNotes.text?.toString().orEmpty()
        )
        viewModel.save(input)
    }

    // VR-06 do VR-17.
    private fun renderFieldErrors(errors: ItemValidator.Errors) {
        binding.layoutName.error = errors.name?.let(::getString)
        binding.layoutCategory.error = errors.category?.let(::getString)
        binding.layoutLocation.error = errors.location?.let(::getString)
        binding.layoutQuantity.error = errors.quantity?.let(::getString)
        binding.layoutPurchasePrice.error = errors.purchasePrice?.let(::getString)
        binding.layoutEstimatedValue.error = errors.estimatedValue?.let(::getString)
        binding.layoutCurrency.error = errors.currency?.let(::getString)
        binding.layoutPurchaseDate.error = errors.purchaseDate?.let(::getString)
        binding.layoutWarrantyDate.error = errors.warrantyExpirationDate?.let(::getString)
        binding.layoutSerialNumber.error = errors.serialNumber?.let(::getString)
        binding.layoutDescription.error = errors.description?.let(::getString)
        binding.layoutNotes.error = errors.notes?.let(::getString)
        binding.layoutManufacturer.error = errors.manufacturer?.let(::getString)
        binding.layoutModel.error = errors.model?.let(::getString)
        binding.layoutSeller.error = errors.seller?.let(::getString)

        // Greska u sekciji "Dodatni podaci" odmah otvara tu sekciju da korisnik ne mora da je trazi.
        if (isErrorInMoreSection(errors)) binding.containerMore.isVisible = true
    }

    private fun isErrorInMoreSection(errors: ItemValidator.Errors): Boolean = listOf(
        errors.quantity, errors.purchasePrice, errors.estimatedValue, errors.currency,
        errors.purchaseDate, errors.warrantyExpirationDate, errors.serialNumber,
        errors.description, errors.notes, errors.manufacturer, errors.model, errors.seller
    ).any { it != null }

    private fun renderSaveState(state: Resource<String>?) {
        val loading = state is Resource.Loading
        binding.progressSaving.isVisible = loading
        binding.buttonSave.isEnabled = !loading

        // FR-030 — po cuvanju korisnik se vraca na listu; forma vraca rezultat pozivajucem ekranu.
        if (state is Resource.Success) {
            initialSnapshot = null
            setFragmentResult(RESULT_ITEM_SAVED, bundleOf(RESULT_ITEM_SAVED_ID to state.data))
            findNavController().popBackStack()
        }
    }

    private fun currentSnapshot(): FormSnapshot = FormSnapshot(
        name = binding.editName.text?.toString().orEmpty(),
        categoryId = selectedCategoryId,
        locationId = selectedLocationId,
        description = binding.editDescription.text?.toString().orEmpty(),
        manufacturer = binding.editManufacturer.text?.toString().orEmpty(),
        model = binding.editModel.text?.toString().orEmpty(),
        serialNumber = binding.editSerialNumber.text?.toString().orEmpty(),
        quantity = binding.editQuantity.text?.toString().orEmpty(),
        purchasePrice = binding.editPurchasePrice.text?.toString().orEmpty(),
        estimatedValue = binding.editEstimatedValue.text?.toString().orEmpty(),
        currency = binding.editCurrency.text?.toString().orEmpty(),
        purchaseDate = selectedPurchaseDate,
        warrantyExpirationDate = selectedWarrantyDate,
        seller = binding.editSeller.text?.toString().orEmpty(),
        notes = binding.editNotes.text?.toString().orEmpty(),
        photoChanged = viewModel.pendingImagePath.value != null
    )

    private fun handleBackPressed() {
        val initial = initialSnapshot
        if (initial == null || initial == currentSnapshot()) {
            findNavController().popBackStack()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.additem_discard_dialog_title)
            .setMessage(R.string.additem_discard_dialog_message)
            .setPositiveButton(R.string.additem_discard_dialog_confirm) { _, _ ->
                // Novoodabrana fotografija se odbacuje zajedno sa ostatkom forme da ne ostane na disku.
                viewModel.discardPendingPhoto()
                findNavController().popBackStack()
            }
            .setNegativeButton(R.string.additem_discard_dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class FormSnapshot(
        val name: String,
        val categoryId: String?,
        val locationId: String?,
        val description: String,
        val manufacturer: String,
        val model: String,
        val serialNumber: String,
        val quantity: String,
        val purchasePrice: String,
        val estimatedValue: String,
        val currency: String,
        val purchaseDate: LocalDate?,
        val warrantyExpirationDate: LocalDate?,
        val seller: String,
        val notes: String,
        val photoChanged: Boolean
    )
}
