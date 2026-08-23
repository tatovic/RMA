package rs.homeinventory.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.databinding.ActivityMainBinding
import javax.inject.Inject

// ACT-2 — ljuska glavnog dela aplikacije dolazi u tiketu 12; ovde se samo bira pocetni ekran (FR-011).
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (authRepository.hasValidSession()) {
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)
            } else {
                startActivity(Intent(this@MainActivity, AuthenticationActivity::class.java))
                finish()
            }
        }
    }
}
