package rs.homeinventory.app.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.databinding.FragmentProfileBinding
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.ui.AuthenticationActivity

// SCR-09 — prikaz podataka i odjava (tiket 12); izmena profila i ulaz u administraciju dolaze kasnije.
@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        binding.buttonLocations.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_locationsFragment)
        }

        // BR-008 — destruktivna akcija trazi potvrdu.
        binding.buttonLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_logout_dialog_title)
                .setMessage(R.string.profile_logout_dialog_message)
                .setPositiveButton(R.string.profile_logout_dialog_confirm) { _, _ -> viewModel.logout() }
                .setNegativeButton(R.string.profile_logout_dialog_cancel, null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.user.collect(::renderUser) }
                launch { viewModel.loggedOut.collect { if (it) goToAuthentication() } }
            }
        }
    }

    private fun renderUser(user: User?) {
        user ?: return
        binding.textName.text = user.name
        binding.textEmail.text = user.email
        binding.textRole.setText(
            if (user.role == UserRole.ADMIN) R.string.profile_role_admin else R.string.profile_role_user
        )
    }

    // BR-005 — posle odjave povratno dugme ne vraca u aplikaciju.
    private fun goToAuthentication() {
        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
