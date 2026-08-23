package rs.homeinventory.app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentRegisterBinding
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.ui.MainActivity
import rs.homeinventory.app.util.AuthValidator
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.Resource

// SCR-02.
@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    // K-05.
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.buttonRegister.setOnClickListener {
            viewModel.register(
                binding.editName.text?.toString().orEmpty(),
                binding.editEmail.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty(),
                binding.editConfirmPassword.text?.toString().orEmpty()
            )
        }

        binding.textGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Ciscenje greske na polju cim korisnik pocne da je ispravlja.
        binding.editName.doOnTextChanged { _, _, _, _ -> binding.layoutName.error = null }
        binding.editEmail.doOnTextChanged { _, _, _, _ -> binding.layoutEmail.error = null }
        binding.editPassword.doOnTextChanged { _, _, _, _ -> binding.layoutPassword.error = null }
        binding.editConfirmPassword.doOnTextChanged { _, _, _, _ -> binding.layoutConfirmPassword.error = null }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.fieldErrors.collect(::renderFieldErrors) }
                launch { viewModel.state.collect(::render) }
            }
        }
    }

    // VR-01 do VR-05.
    private fun renderFieldErrors(errors: AuthValidator.RegisterErrors) {
        binding.layoutName.error = errors.name?.let(::getString)
        binding.layoutEmail.error = errors.email?.let(::getString)
        binding.layoutPassword.error = errors.password?.let(::getString)
        binding.layoutConfirmPassword.error = errors.confirmPassword?.let(::getString)
    }

    private fun render(state: Resource<User>?) {
        val loading = state is Resource.Loading
        binding.progress.isVisible = loading
        binding.buttonRegister.isEnabled = !loading

        when {
            state is Resource.Error && state.code == ErrorCode.EMAIL_ALREADY_EXISTS -> {
                // VR-03 — jedinstvenost email-a se proverava na serveru, greska ide ispod polja za email.
                binding.textError.isVisible = false
                binding.layoutEmail.error = state.message
            }
            state is Resource.Error -> {
                binding.textError.text = state.message
                binding.textError.isVisible = true
            }
            else -> binding.textError.isVisible = false
        }

        // FR-018 — uspesna registracija odmah prijavljuje korisnika, bez dodatnog koraka.
        if (state is Resource.Success) {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
