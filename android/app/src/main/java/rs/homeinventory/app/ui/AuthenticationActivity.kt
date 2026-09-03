package rs.homeinventory.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.databinding.ActivityAuthenticationBinding
import rs.homeinventory.app.util.applySystemBarsPadding

// ACT-1 — ulazna tacka kad nema vazece sesije (FR-011). Sadrzi nav_auth.xml sa Login i Register ekranima.
@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding() // NFR-10, tiket 28
    }
}
