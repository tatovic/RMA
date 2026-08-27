package rs.homeinventory.app.presentation.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentDashboardBinding
import rs.homeinventory.app.databinding.ItemDashboardCategoryBinding
import rs.homeinventory.app.databinding.ItemDashboardRecentBinding
import rs.homeinventory.app.util.UiState

// SCR-03 — jedini izvor podataka za ekran je Room (tech.md sekcija 5.3); cetiri stanja po BR-017.
@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        // BR-017 — poziv na akciju umesto prazne beline; dodavanje predmeta ide preko FAB-a na SCR-04.
        binding.buttonEmptyAction.setOnClickListener {
            findNavController().navigate(R.id.inventoryFragment)
        }
        binding.buttonErrorRetry.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.isRefreshing.collect { binding.swipeRefresh.isRefreshing = it } }
                // ERR-05 — greska mreze uz postojece lokalne podatke ide kao kratka poruka, ne ceo ekran greske.
                launch { viewModel.snackbarMessage.collect(::showRetrySnackbar) }
            }
        }
    }

    private fun render(state: UiState<DashboardUi>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.swipeRefresh.isVisible = state is UiState.Success
        binding.groupEmpty.isVisible = state is UiState.Empty
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> renderContent(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    private fun renderContent(data: DashboardUi) {
        binding.textTotalItemsValue.text = data.totalItemCount.toString()
        binding.textTotalValue.text = data.totalValueFormatted
        binding.textUnconvertedNote.isVisible = data.hasUnconvertedCurrencies

        binding.containerCategoryBreakdown.removeAllViews()
        data.categoryCounts.forEach { category ->
            val row = ItemDashboardCategoryBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerCategoryBreakdown, false
            )
            row.textCategoryName.text = category.categoryName
            row.textCategoryCount.text = category.itemCount.toString()
            binding.containerCategoryBreakdown.addView(row.root)
        }

        binding.containerRecentItems.removeAllViews()
        data.recentItems.forEach { item ->
            val row = ItemDashboardRecentBinding.inflate(
                LayoutInflater.from(requireContext()), binding.containerRecentItems, false
            )
            row.textItemName.text = item.name
            row.textItemSubtitle.text =
                getString(R.string.dashboard_recent_subtitle, item.categoryName, item.locationName)
            row.textItemValue.text = item.valueFormatted
            binding.containerRecentItems.addView(row.root)
        }
    }

    private fun showRetrySnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.dashboard_error_retry_action) { viewModel.refresh() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
