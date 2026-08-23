package rs.homeinventory.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.databinding.ActivityItemDetailsBinding

// ACT-3 — sadrzi nav_details.xml sa ItemDetails i AddEditItem ekranima (tech.md sekcija 10).
// Ucitavanje predmeta po itemId (BR-007) dolazi u tiketu 16.
@AndroidEntryPoint
class ItemDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
