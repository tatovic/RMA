package rs.homeinventory.app.presentation.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentAdminDashboardBinding
import rs.homeinventory.app.util.UiState

// SCR-11 — pregled sistema: broj korisnika po statusu, ukupan broj predmeta i kategorija (tiket 25).
@AndroidEntryPoint
class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminDashboardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminDashboardBinding.bind(view)

        binding.rowRegisteredUsers.textStatLabel.setText(R.string.admin_dashboard_registered_users)
        binding.rowActiveUsers.textStatLabel.setText(R.string.admin_dashboard_active_users)
        binding.rowDeactivatedUsers.textStatLabel.setText(R.string.admin_dashboard_deactivated_users)
        binding.rowTotalItems.textStatLabel.setText(R.string.admin_dashboard_total_items)
        binding.rowTotalCategories.textStatLabel.setText(R.string.admin_dashboard_total_categories)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.buttonErrorRetry.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.isRefreshing.collect { binding.swipeRefresh.isRefreshing = it } }
            }
        }
    }

    private fun render(state: UiState<AdminStatsUi>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.swipeRefresh.isVisible = state is UiState.Success
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> renderContent(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    private fun renderContent(data: AdminStatsUi) {
        binding.rowRegisteredUsers.textStatValue.text = data.registeredUsers.toString()
        binding.rowActiveUsers.textStatValue.text = data.activeUsers.toString()
        binding.rowDeactivatedUsers.textStatValue.text = data.deactivatedUsers.toString()
        binding.rowTotalItems.textStatValue.text = data.totalItems.toString()
        binding.rowTotalCategories.textStatValue.text = data.totalCategories.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
