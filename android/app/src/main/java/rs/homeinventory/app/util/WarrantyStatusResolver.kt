package rs.homeinventory.app.util

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import rs.homeinventory.app.R
import rs.homeinventory.app.domain.model.WarrantyStatus

// BR-010 — svaki status garancije ima svoju boju i tekst, razdvojeni na jednom mestu (tiket 22).
@ColorRes
fun warrantyStatusColorRes(status: WarrantyStatus): Int = when (status) {
    WarrantyStatus.AKTIVNA -> R.color.warranty_status_active
    WarrantyStatus.USKORO_ISTICE -> R.color.warranty_status_expiring_soon
    WarrantyStatus.ISTEKLA -> R.color.warranty_status_expired
    WarrantyStatus.NEPOZNATO -> R.color.warranty_status_unknown
}

@StringRes
fun warrantyStatusLabelRes(status: WarrantyStatus): Int = when (status) {
    WarrantyStatus.AKTIVNA -> R.string.warranty_status_active
    WarrantyStatus.USKORO_ISTICE -> R.string.warranty_status_expiring_soon
    WarrantyStatus.ISTEKLA -> R.string.warranty_status_expired
    WarrantyStatus.NEPOZNATO -> R.string.warranty_status_unknown
}
