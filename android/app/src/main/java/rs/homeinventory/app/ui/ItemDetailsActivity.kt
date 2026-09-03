package rs.homeinventory.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.ActivityItemDetailsBinding
import rs.homeinventory.app.util.EXTRA_ITEM_ID
import rs.homeinventory.app.util.applySystemBarsPadding

// ACT-3 — sadrzi nav_details.xml sa ItemDetails i AddEditItem ekranima (tech.md sekcija 10).
// itemId (BR-007) stize kroz Intent extra i prosledjuje se grafu kao argument pocetne destinacije.
@AndroidEntryPoint
class ItemDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding() // NFR-10, tiket 28

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        if (itemId == null) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            navHostFragment.navController.setGraph(R.navigation.nav_details, bundleOf("itemId" to itemId))
        }
    }
}
