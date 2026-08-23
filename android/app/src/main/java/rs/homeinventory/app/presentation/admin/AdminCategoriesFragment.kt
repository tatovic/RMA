package rs.homeinventory.app.presentation.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentPlaceholderBinding

// SCR-13 — sadrzaj dolazi u tiketu 25.
@AndroidEntryPoint
class AdminCategoriesFragment : Fragment(R.layout.fragment_placeholder) {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceholderBinding.bind(view)
        binding.textPlaceholderTitle.setText(R.string.nav_admin_categories)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
