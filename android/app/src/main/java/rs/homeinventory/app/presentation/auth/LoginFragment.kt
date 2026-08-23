package rs.homeinventory.app.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentLoginBinding
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.ui.MainActivity
import rs.homeinventory.app.util.Resource

// SCR-01.
@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    // K-05.
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.buttonLogin.setOnClickListener {
            viewModel.login(
                binding.editEmail.text?.toString().orEmpty(),
                binding.editPassword.text?.toString().orEmpty()
            )
        }

        binding.textGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: Resource<User>?) {
        val loading = state is Resource.Loading
        binding.progress.isVisible = loading
        binding.buttonLogin.isEnabled = !loading

        if (state is Resource.Error) {
            binding.textError.text = state.message
            binding.textError.isVisible = true
        } else {
            binding.textError.isVisible = false
        }

        // FR-018/back-stack: posle uspesne prijave se ne moze vratiti nazad na login.
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
