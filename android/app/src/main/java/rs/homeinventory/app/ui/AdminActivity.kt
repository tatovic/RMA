package rs.homeinventory.app.ui

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.databinding.ActivityAdminBinding
import javax.inject.Inject

// ACT-4 — sadrzi nav_admin.xml, dostupna samo roli ADMIN (tech.md sekcija 10, tiket 25). Ulaz iz
// profila (SCR-09) vec skriva dugme za korisnika bez role ADMIN; ovo je odbrana u dubinu za slucaj
// direktnog pokretanja aktivnosti (npr. povratak sistemskim taskovima).
@AndroidEntryPoint
class AdminActivity : AppCompatActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (userPreferences.role.first() == UserRole.ADMIN.name) {
                binding = ActivityAdminBinding.inflate(layoutInflater)
                setContentView(binding.root)
                setupNavigation()
            } else {
                finish()
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        binding.bottomNavigation.setupWithNavController(navHostFragment.navController)

        // Sva tri ekrana su top-level (nema ekvivalenta SCR-06/SCR-10 iz MainActivity), pa traka
        // ostaje stalno vidljiva — sadrzaj samo dobija trajnu marginu da FAB/dugmad ne zavrse ispod nje.
        binding.bottomNavigation.doOnLayout {
            binding.navHostFragment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = binding.bottomNavigation.height
            }
        }
    }
}
