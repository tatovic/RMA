package rs.homeinventory.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.databinding.ActivityMainBinding
import javax.inject.Inject

// ACT-2 — ljuska glavnog dela aplikacije: bottom navigacija sa nav_main.xml (tech.md sekcija 10).
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    private lateinit var binding: ActivityMainBinding

    // SCR-06 i SCR-10 nisu u meniju — bottom bar se sakriva dok su otvoreni.
    private val topLevelDestinationIds = setOf(
        R.id.dashboardFragment,
        R.id.inventoryFragment,
        R.id.statisticsFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (authRepository.hasValidSession()) {
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)
                setupNavigation()
            } else {
                startActivity(Intent(this@MainActivity, AuthenticationActivity::class.java))
                finish()
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Sadrzaj ne sme da zavrsi ispod bottom bara — margina se racuna iz stvarne
        // izmerene visine, ne iz pretpostavljene konstante.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in topLevelDestinationIds
            binding.bottomNavigation.isVisible = isTopLevel
            if (isTopLevel) {
                binding.bottomNavigation.doOnLayout {
                    setContentBottomMargin(binding.bottomNavigation.height)
                }
            } else {
                setContentBottomMargin(0)
            }
        }
    }

    private fun setContentBottomMargin(margin: Int) {
        binding.navHostFragment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = margin
        }
    }
}
