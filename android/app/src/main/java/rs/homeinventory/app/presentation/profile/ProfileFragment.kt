package rs.homeinventory.app.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.databinding.FragmentProfileBinding
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.ui.AdminActivity
import rs.homeinventory.app.ui.AuthenticationActivity

// SCR-09 — prikaz podataka i odjava (tiket 12); izmena profila dolazi kasnije, ulaz u administraciju u tiketu 25.
@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        binding.editCurrency.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, viewModel.supportedCurrencies)
        )
        // Promena valute prikaza odmah preracunava sve zbirove u aplikaciji (currentUser flow).
        binding.editCurrency.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateCurrency(viewModel.supportedCurrencies[position])
        }

        // FR-051/FR-052 — izbor praga odmah menja prikaz svuda jer WarrantyPreferences.thresholdDays
        // se cita reaktivno (tiket 22).
        val thresholdLabels = viewModel.warrantyThresholdOptions.map {
            getString(R.string.warranty_threshold_days_format, it)
        }
        binding.editWarrantyThreshold.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, thresholdLabels)
        )
        binding.editWarrantyThreshold.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateWarrantyThreshold(viewModel.warrantyThresholdOptions[position])
        }

        val themeLabels = listOf(
            R.string.theme_option_system, R.string.theme_option_light, R.string.theme_option_dark
        ).map(::getString)
        binding.editTheme.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, themeLabels)
        )
        binding.editTheme.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateNightMode(viewModel.nightModeOptions[position])
        }

        binding.buttonLocations.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_locationsFragment)
        }

        // FR-100 — ulaz u administraciju.
        binding.buttonAdmin.setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
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
                launch { viewModel.currencyUpdateError.collect(::showCurrencyUpdateError) }
                launch { viewModel.warrantyThresholdDays.collect(::renderWarrantyThreshold) }
                launch { viewModel.nightMode.collect(::renderNightMode) }
                launch { viewModel.isSavingCurrency.collect(::renderSavingCurrency) }
            }
        }
    }

    // BR-017 Loading/Success — kratak trenutak dok currentUser Flow prvi put ne emituje (tiket 27).
    private fun renderUser(user: User?) {
        binding.progressLoading.isVisible = user == null
        val contentViews = listOf(
            binding.textName, binding.textEmail, binding.textRole,
            binding.layoutCurrency, binding.layoutWarrantyThreshold, binding.layoutTheme,
            binding.buttonLocations, binding.buttonLogout
        )
        contentViews.forEach { it.isVisible = user != null }
        user ?: return
        binding.textName.text = user.name
        binding.textEmail.text = user.email
        binding.textRole.setText(
            if (user.role == UserRole.ADMIN) R.string.profile_role_admin else R.string.profile_role_user
        )
        // FR-100 — dugme za administraciju vidljivo samo roli ADMIN.
        binding.buttonAdmin.isVisible = user.role == UserRole.ADMIN
        // false — postavlja tekst bez ponovnog filtriranja padajuce liste.
        binding.editCurrency.setText(user.currency, false)
    }

    // false — postavlja tekst bez ponovnog filtriranja padajuce liste.
    private fun renderWarrantyThreshold(days: Int) {
        binding.editWarrantyThreshold.setText(getString(R.string.warranty_threshold_days_format, days), false)
    }

    // false — postavlja tekst bez ponovnog filtriranja padajuce liste.
    private fun renderNightMode(mode: Int) {
        val position = viewModel.nightModeOptions.indexOf(mode).coerceAtLeast(0)
        binding.editTheme.setText(binding.editTheme.adapter?.getItem(position) as? String, false)
    }

    private fun showCurrencyUpdateError(message: String?) {
        message ?: return
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    // prd.md sekcija 11 — dropdown se onemogucava dok promena valute putuje na server (sprecava dvostruko slanje).
    private fun renderSavingCurrency(isSaving: Boolean) {
        binding.layoutCurrency.isEnabled = !isSaving
        binding.editCurrency.isEnabled = !isSaving
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
