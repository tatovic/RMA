package rs.homeinventory.app.presentation.itemdetails

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentPlaceholderBinding

// SCR-07 — sadrzaj dolazi u tiketu 16.
@AndroidEntryPoint
class ItemDetailsFragment : Fragment(R.layout.fragment_placeholder) {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceholderBinding.bind(view)
        binding.textPlaceholderTitle.setText(R.string.nav_item_details)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
