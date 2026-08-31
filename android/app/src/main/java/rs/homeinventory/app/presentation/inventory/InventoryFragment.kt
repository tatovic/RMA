package rs.homeinventory.app.presentation.inventory

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentInventoryBinding
import rs.homeinventory.app.ui.ItemDetailsActivity
import rs.homeinventory.app.util.EXTRA_ITEM_ID
import rs.homeinventory.app.util.RESULT_ITEM_SAVED
import rs.homeinventory.app.util.UiState

// SCR-04 — lista se osvezava sama iz Room-a, cetiri stanja po BR-017 (tech.md sekcija 5.4).
@AndroidEntryPoint
class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    private val adapter = InventoryItemAdapter(::openItemDetails)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInventoryBinding.bind(view)

        binding.recyclerItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerItems.adapter = adapter
        binding.recyclerItems.setHasFixedSize(true)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        binding.fabAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_addEditItemFragment)
        }
        // Prazan inventar poziva na dodavanje prvog predmeta istom akcijom kao FAB.
        binding.buttonEmptyAction.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_addEditItemFragment)
        }
        binding.buttonErrorRetry.setOnClickListener { viewModel.refresh() }

        // Forma za dodavanje/izmenu vraca rezultat ovde (tiket 15) — kratka potvrda korisniku.
        setFragmentResultListener(RESULT_ITEM_SAVED) { _, _ ->
            Snackbar.make(binding.root, R.string.inventory_item_saved_confirmation, Snackbar.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.isRefreshing.collect { binding.swipeRefresh.isRefreshing = it } }
                // ERR-05 — greska mreze uz postojece lokalne podatke ide kao kratka poruka, ne ceo ekran greske.
                launch { viewModel.snackbarMessage.collect(::showRetrySnackbar) }
            }
        }
    }

    private fun render(state: UiState<List<InventoryItemUi>>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.swipeRefresh.isVisible = state is UiState.Success
        binding.groupEmpty.isVisible = state is UiState.Empty
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> adapter.submitList(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    // SCR-06 dolazi u tiketu 16; klik otvara detalje i dok je taj ekran jos prazan (BR-007 — samo id se prosledjuje).
    private fun openItemDetails(item: InventoryItemUi) {
        startActivity(
            Intent(requireContext(), ItemDetailsActivity::class.java)
                .putExtra(EXTRA_ITEM_ID, item.id)
        )
    }

    private fun showRetrySnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.inventory_error_retry_action) { viewModel.refresh() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerItems.adapter = null
        _binding = null
    }
}
