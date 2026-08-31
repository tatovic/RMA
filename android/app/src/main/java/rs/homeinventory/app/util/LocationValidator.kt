package rs.homeinventory.app.util

import androidx.annotation.StringRes
import rs.homeinventory.app.R

// Klijentska validacija forme lokacije — VR-19, backend/src/modules/locations/locations.schema.js.
object LocationValidator {

    data class Input(val name: String, val description: String)

    data class Errors(
        @StringRes val name: Int? = null,
        @StringRes val description: Int? = null
    ) {
        val isValid: Boolean get() = name == null && description == null
    }

    data class Parsed(val name: String, val description: String?)

    fun validate(input: Input): Pair<Errors, Parsed?> {
        val name = input.name.trim()
        val nameError = if (name.length !in 1..60) R.string.error_vr_location_name else null

        val description = input.description.trim()
        val descriptionError = if (description.length > 255) R.string.error_vr_location_description else null

        val errors = Errors(name = nameError, description = descriptionError)
        if (!errors.isValid) return errors to null

        return errors to Parsed(name = name, description = description.ifBlank { null })
    }
}
