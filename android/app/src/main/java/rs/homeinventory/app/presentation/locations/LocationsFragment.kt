package rs.homeinventory.app.presentation.locations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentPlaceholderBinding

// SCR-10 — sadrzaj dolazi u tiketu 17.
@AndroidEntryPoint
class LocationsFragment : Fragment(R.layout.fragment_placeholder) {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceholderBinding.bind(view)
        binding.textPlaceholderTitle.setText(R.string.nav_locations)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
