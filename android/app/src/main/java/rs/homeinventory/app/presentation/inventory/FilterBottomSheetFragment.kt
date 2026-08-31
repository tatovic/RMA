package rs.homeinventory.app.presentation.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.databinding.FragmentFilterBottomSheetBinding

// SCR-05 — panel filtera i sortiranja, otvara se iz SCR-04 (tiket 20). Koristi ISTI InventoryViewModel
// kao roditeljski InventoryFragment (deljeno stanje preko requireParentFragment()), pa se promena
// filtera/sortiranja ovde odmah odrazava na listu iza panela.
@AndroidEntryPoint
class FilterBottomSheetFragment : BottomSheetDialogFragment(R.layout.fragment_filter_bottom_sheet) {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels({ requireParentFragment() })

    private var renderedCategoryIds: List<String> = emptyList()
    private var renderedLocationIds: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFilterBottomSheetBinding.bind(view)

        applyInitialState()

        binding.radioGroupSort.setOnCheckedChangeListener { _, checkedId ->
            viewModel.onSortModeSelected(sortModeForRadioId(checkedId))
        }
        binding.editMinPrice.doAfterTextChanged { viewModel.onMinPriceChanged(it?.toString().orEmpty()) }
        binding.editMaxPrice.doAfterTextChanged { viewModel.onMaxPriceChanged(it?.toString().orEmpty()) }
        binding.editPurchaseYear.doAfterTextChanged { viewModel.onPurchaseYearChanged(it?.toString().orEmpty()) }
        binding.checkUnderWarranty.setOnCheckedChangeListener { _, checked ->
            viewModel.onUnderWarrantyOnlyChanged(checked)
        }
        binding.checkWarrantyExpiringSoon.setOnCheckedChangeListener { _, checked ->
            viewModel.onWarrantyExpiringSoonOnlyChanged(checked)
        }
        binding.buttonResetFilters.setOnClickListener { resetAllFilters() }
        binding.buttonDone.setOnClickListener { dismiss() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.categories.collect(::renderCategoryChips) }
                launch { viewModel.locations.collect(::renderLocationChips) }
                launch { viewModel.userCurrency.collect(::applyCurrencyHints) }
            }
        }
    }

    // Panel se otvara sa vec postojecim stanjem iz deljenog ViewModel-a (npr. posle rotacije ili
    // ako je vec ranije podesen filter) - jednokratno popunjavanje, ne StateFlow kolekcija, da se
    // korisnikov unos u toku ne prepisuje ponovnom emisijom istog stanja.
    private fun applyInitialState() {
        binding.editMinPrice.setText(viewModel.minPriceText.value)
        binding.editMaxPrice.setText(viewModel.maxPriceText.value)
        binding.editPurchaseYear.setText(viewModel.purchaseYearText.value)

        val filters = viewModel.filterState.value
        binding.checkUnderWarranty.isChecked = filters.underWarrantyOnly
        binding.checkWarrantyExpiringSoon.isChecked = filters.warrantyExpiringSoonOnly
        binding.radioGroupSort.check(radioIdForSortMode(viewModel.sortMode.value))
    }

    private fun sortModeForRadioId(id: Int): InventorySortMode = when (id) {
        R.id.radioSortOldest -> InventorySortMode.OLDEST
        R.id.radioSortNameAsc -> InventorySortMode.NAME_ASC
        R.id.radioSortNameDesc -> InventorySortMode.NAME_DESC
        R.id.radioSortPriceAsc -> InventorySortMode.PRICE_ASC
        R.id.radioSortPriceDesc -> InventorySortMode.PRICE_DESC
        else -> InventorySortMode.NEWEST
    }

    private fun radioIdForSortMode(mode: InventorySortMode): Int = when (mode) {
        InventorySortMode.NEWEST -> R.id.radioSortNewest
        InventorySortMode.OLDEST -> R.id.radioSortOldest
        InventorySortMode.NAME_ASC -> R.id.radioSortNameAsc
        InventorySortMode.NAME_DESC -> R.id.radioSortNameDesc
        InventorySortMode.PRICE_ASC -> R.id.radioSortPriceAsc
        InventorySortMode.PRICE_DESC -> R.id.radioSortPriceDesc
    }

    // FR-033 — kategorija, vise izbora. ChipGroup se ponovo gradi samo kad se stvarno promeni skup
    // kategorija, da ne bi obrisao korisnikov trenutni izbor pri svakoj (identicnoj) ponovnoj emisiji.
    private fun renderCategoryChips(categories: List<CategoryEntity>) {
        val ids = categories.map { it.id }
        if (ids == renderedCategoryIds) return
        renderedCategoryIds = ids

        val selected = viewModel.filterState.value.categoryIds
        val inflater = LayoutInflater.from(requireContext())
        binding.chipGroupCategories.removeAllViews()
        categories.forEach { category ->
            val chip = inflater.inflate(
                R.layout.item_filter_chip, binding.chipGroupCategories, false
            ) as Chip
            chip.text = category.name
            chip.tag = category.id
            chip.isChecked = category.id in selected
            chip.setOnCheckedChangeListener { _, _ -> onCategorySelectionChanged() }
            binding.chipGroupCategories.addView(chip)
        }
    }

    // FR-034 — lokacija, vise izbora.
    private fun renderLocationChips(locations: List<LocationEntity>) {
        val ids = locations.map { it.id }
        if (ids == renderedLocationIds) return
        renderedLocationIds = ids

        val selected = viewModel.filterState.value.locationIds
        val inflater = LayoutInflater.from(requireContext())
        binding.chipGroupLocations.removeAllViews()
        locations.forEach { location ->
            val chip = inflater.inflate(
                R.layout.item_filter_chip, binding.chipGroupLocations, false
            ) as Chip
            chip.text = location.name
            chip.tag = location.id
            chip.isChecked = location.id in selected
            chip.setOnCheckedChangeListener { _, _ -> onLocationSelectionChanged() }
            binding.chipGroupLocations.addView(chip)
        }
    }

    private fun onCategorySelectionChanged() {
        val selected = binding.chipGroupCategories.children
            .filterIsInstance<Chip>()
            .filter { it.isChecked }
            .map { it.tag as String }
            .toSet()
        viewModel.onCategoryFilterChanged(selected)
    }

    private fun onLocationSelectionChanged() {
        val selected = binding.chipGroupLocations.children
            .filterIsInstance<Chip>()
            .filter { it.isChecked }
            .map { it.tag as String }
            .toSet()
        viewModel.onLocationFilterChanged(selected)
    }

    // BR-009 — polja za raspon cene su oznacena valutom prikaza korisnika, ne sirovim brojem.
    private fun applyCurrencyHints(currency: String?) {
        val code = currency.orEmpty()
        binding.layoutMinPrice.hint = getString(R.string.inventory_filter_price_from, code)
        binding.layoutMaxPrice.hint = getString(R.string.inventory_filter_price_to, code)
    }

    // Dugme "Ponisti sve filtere" — sortiranje se namerno ne dira, ono nije filter.
    private fun resetAllFilters() {
        binding.chipGroupCategories.children.filterIsInstance<Chip>().forEach { it.isChecked = false }
        binding.chipGroupLocations.children.filterIsInstance<Chip>().forEach { it.isChecked = false }
        binding.editMinPrice.text = null
        binding.editMaxPrice.text = null
        binding.editPurchaseYear.text = null
        binding.checkUnderWarranty.isChecked = false
        binding.checkWarrantyExpiringSoon.isChecked = false
        viewModel.onResetFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
    }
}
