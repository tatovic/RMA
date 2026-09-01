package rs.homeinventory.app.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.homeinventory.app.databinding.ItemAdminCategoryBinding
import rs.homeinventory.app.presentation.locations.formatItemCount

// Poredjenje po id-u da lista ne trepe pri osvezavanju, isti obrazac kao LocationsAdapter (tiket 17).
class AdminCategoriesAdapter(
    private val onEditClick: (AdminCategoryUi) -> Unit,
    private val onDeleteClick: (AdminCategoryUi) -> Unit
) : ListAdapter<AdminCategoryUi, AdminCategoriesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAdminCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: AdminCategoryUi) {
            binding.textCategoryName.text = category.name
            binding.textCategoryCount.text = formatItemCount(binding.root.context, category.itemCount)

            binding.buttonEditCategory.setOnClickListener { onEditClick(category) }
            binding.buttonDeleteCategory.setOnClickListener { onDeleteClick(category) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AdminCategoryUi>() {
        override fun areItemsTheSame(oldItem: AdminCategoryUi, newItem: AdminCategoryUi) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AdminCategoryUi, newItem: AdminCategoryUi) = oldItem == newItem
    }
}
