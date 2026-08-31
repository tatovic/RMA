package rs.homeinventory.app.presentation.locations

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.homeinventory.app.R
import rs.homeinventory.app.databinding.ItemLocationBinding

// Poredjenje po id-u da lista ne trepe pri osvezavanju, isti obrazac kao InventoryItemAdapter (tiket 14).
class LocationsAdapter(
    private val onEditClick: (LocationUi) -> Unit,
    private val onDeleteClick: (LocationUi) -> Unit
) : ListAdapter<LocationUi, LocationsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: LocationUi) {
            binding.textLocationName.text = location.name

            binding.textLocationDescription.isVisible = location.description != null
            binding.textLocationDescription.text = location.description

            // FR-049 — broj predmeta uz svaku lokaciju.
            binding.textLocationCount.text = formatItemCount(binding.root.context, location.itemCount)

            binding.buttonEditLocation.setOnClickListener { onEditClick(location) }
            binding.buttonDeleteLocation.setOnClickListener { onDeleteClick(location) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LocationUi>() {
        override fun areItemsTheSame(oldItem: LocationUi, newItem: LocationUi) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LocationUi, newItem: LocationUi) = oldItem == newItem
    }
}

// Srpski ima tri oblika za broj predmeta; bez <plurals> resursa (nedosledno za sr-Latn/Cyrl), biramo rucno.
fun formatItemCount(context: Context, count: Int): String = when (count) {
    0 -> context.getString(R.string.locations_item_count_zero)
    1 -> context.getString(R.string.locations_item_count_one)
    else -> context.getString(R.string.locations_item_count_many, count)
}
