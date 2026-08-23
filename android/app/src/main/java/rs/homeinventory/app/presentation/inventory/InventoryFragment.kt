package rs.homeinventory.app.presentation.inventory

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.FragmentInventoryBinding

// SCR-04 — sadrzaj dolazi u tiketu 14; FAB ovde dokazuje navigaciju ka SCR-06 (tech.md sekcija 10).
@AndroidEntryPoint
class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInventoryBinding.bind(view)

        binding.fabAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_addEditItemFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
