package rs.homeinventory.app.presentation.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.ItemInventoryBinding
import rs.homeinventory.app.util.photoFile
import rs.homeinventory.app.util.resolveCategoryIcon

// Poredjenje po id-u da lista ne trepe pri osvezavanju (tiket 14); ListAdapter + DiffUtil
// racunaju razliku na pozadinskoj niti, sto drzi skrolovanje glatkim i na 500 predmeta (NFR-01).
class InventoryItemAdapter(
    private val onItemClick: (InventoryItemUi) -> Unit
) : ListAdapter<InventoryItemUi, InventoryItemAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItemUi) {
            binding.textItemName.text = item.name
            binding.textItemSubtitle.text = binding.root.context.getString(
                R.string.inventory_item_subtitle, item.categoryName, item.locationName
            )
            binding.textItemValue.text = item.priceFormatted
            binding.imageItem.contentDescription = item.categoryName

            // FR-087 — bez fotografije prikazuje se ikonica kategorije umesto prazne povrsine.
            if (item.imagePath != null) {
                Glide.with(binding.imageItem)
                    .load(photoFile(binding.root.context, item.imagePath))
                    .placeholder(resolveCategoryIcon(item.categoryIconKey))
                    .error(resolveCategoryIcon(item.categoryIconKey))
                    .centerCrop()
                    .into(binding.imageItem)
            } else {
                Glide.with(binding.imageItem).clear(binding.imageItem)
                binding.imageItem.setImageResource(resolveCategoryIcon(item.categoryIconKey))
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InventoryItemUi>() {
        override fun areItemsTheSame(oldItem: InventoryItemUi, newItem: InventoryItemUi) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryItemUi, newItem: InventoryItemUi) =
            oldItem == newItem
    }
}
