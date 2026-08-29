package rs.homeinventory.app.util

import androidx.annotation.DrawableRes
import rs.homeinventory.app.R

// FR-087 — ikonica kategorije zamenjuje praznu povrsinu kad predmet nema fotografiju.
// iconKey stize sa servera i odgovara nazivu drawable resursa (backend/src/db/schema.sql).
@DrawableRes
fun resolveCategoryIcon(iconKey: String?): Int = when (iconKey) {
    "ic_category_electronics" -> R.drawable.ic_category_electronics
    "ic_category_furniture" -> R.drawable.ic_category_furniture
    "ic_category_appliances" -> R.drawable.ic_category_appliances
    "ic_category_kitchen" -> R.drawable.ic_category_kitchen
    "ic_category_clothing" -> R.drawable.ic_category_clothing
    "ic_category_tools" -> R.drawable.ic_category_tools
    "ic_category_sports" -> R.drawable.ic_category_sports
    "ic_category_vehicles" -> R.drawable.ic_category_vehicles
    "ic_category_decor" -> R.drawable.ic_category_decor
    "ic_category_documents" -> R.drawable.ic_category_documents
    else -> R.drawable.ic_category_other
}
