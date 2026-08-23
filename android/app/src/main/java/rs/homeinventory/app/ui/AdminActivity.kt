package rs.homeinventory.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import rs.homeinventory.app.databinding.ActivityAdminBinding

// ACT-4 — sadrzi nav_admin.xml, dostupna samo roli ADMIN (tech.md sekcija 10). Sadrzaj i ulaz iz profila dolaze u tiketu 25.
@AndroidEntryPoint
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
