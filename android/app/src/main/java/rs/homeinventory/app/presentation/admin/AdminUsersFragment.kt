package rs.homeinventory.app.presentation.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentAdminUsersBinding
import rs.homeinventory.app.util.UiState

// SCR-12 — spisak korisnika sa promenom statusa, uz dijalog potvrde (BR-008, tiket 25).
@AndroidEntryPoint
class AdminUsersFragment : Fragment(R.layout.fragment_admin_users) {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminUsersViewModel by viewModels()

    private val adapter = AdminUsersAdapter(::onStatusToggle)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminUsersBinding.bind(view)

        binding.recyclerAdminUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAdminUsers.adapter = adapter
        binding.recyclerAdminUsers.setHasFixedSize(true)

        binding.buttonErrorRetry.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.statusUpdateError.collect(::showStatusUpdateError) }
            }
        }
    }

    private fun render(state: UiState<List<AdminUserUi>>) {
        binding.progressLoading.isVisible = state is UiState.Loading
        binding.recyclerAdminUsers.isVisible = state is UiState.Success
        binding.groupEmpty.isVisible = state is UiState.Empty
        binding.groupError.isVisible = state is UiState.Error

        when (state) {
            is UiState.Success -> adapter.submitList(state.data)
            is UiState.Error -> binding.textErrorMessage.text = state.message
            UiState.Loading, UiState.Empty -> Unit
        }
    }

    // BR-004 — samodeaktivacija se prijavljuje odmah, bez mreznog poziva; server je i dalje konacna
    // provera (409 CANNOT_DEACTIVATE_SELF) za slucaj da je nalog vec bio deaktiviran sa drugog uredjaja.
    private fun onStatusToggle(user: AdminUserUi, requestedActive: Boolean) {
        if (user.isSelf && !requestedActive) {
            Snackbar.make(binding.root, R.string.error_cannot_deactivate_self, Snackbar.LENGTH_LONG).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (requestedActive) R.string.admin_users_activate_dialog_title
                else R.string.admin_users_deactivate_dialog_title
            )
            .setMessage(
                getString(
                    if (requestedActive) R.string.admin_users_activate_dialog_message
                    else R.string.admin_users_deactivate_dialog_message,
                    user.name
                )
            )
            .setPositiveButton(R.string.admin_users_status_dialog_confirm) { _, _ ->
                viewModel.updateStatus(user.id, requestedActive)
            }
            .setNegativeButton(R.string.admin_users_status_dialog_cancel, null)
            .show()
    }

    private fun showStatusUpdateError(message: String?) {
        message ?: return
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.consumeStatusUpdateError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerAdminUsers.adapter = null
        _binding = null
    }
}
