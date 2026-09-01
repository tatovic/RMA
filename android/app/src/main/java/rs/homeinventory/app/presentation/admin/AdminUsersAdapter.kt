package rs.homeinventory.app.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.homeinventory.app.R
import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.databinding.ItemAdminUserBinding
import rs.homeinventory.app.presentation.locations.formatItemCount

// Poredjenje po id-u da lista ne trepe pri osvezavanju, isti obrazac kao LocationsAdapter (tiket 17).
class AdminUsersAdapter(
    private val onStatusToggle: (AdminUserUi, Boolean) -> Unit
) : ListAdapter<AdminUserUi, AdminUsersAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAdminUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: AdminUserUi) {
            binding.textUserName.text = user.name
            binding.textUserEmail.text = user.email
            binding.textUserRole.setText(
                if (user.role == UserRole.ADMIN) R.string.profile_role_admin else R.string.profile_role_user
            )
            binding.textUserItemCount.text = formatItemCount(binding.root.context, user.itemCount)

            // Prekidac se vraca na trenutno stanje odmah po dodiru — konacna izmena ceka potvrdu
            // dijaloga (BR-008) i uspesan odgovor servera; recikliranje zahteva ciscenje starog listenera.
            binding.switchActive.setOnClickListener(null)
            binding.switchActive.isChecked = user.isActive
            binding.switchActive.setOnClickListener {
                val requestedActive = binding.switchActive.isChecked
                binding.switchActive.isChecked = user.isActive
                onStatusToggle(user, requestedActive)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AdminUserUi>() {
        override fun areItemsTheSame(oldItem: AdminUserUi, newItem: AdminUserUi) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AdminUserUi, newItem: AdminUserUi) = oldItem == newItem
    }
}
