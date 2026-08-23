package rs.homeinventory.app.util

import androidx.annotation.StringRes
import rs.homeinventory.app.R

// Klijentska validacija forme registracije — prd.md sekcija 9, VR-01, VR-02, VR-04, VR-05.
// VR-03 (jedinstvenost email-a) se ne moze proveriti lokalno; dolazi sa servera kao EMAIL_ALREADY_EXISTS
// i prikazuje se na isti nacin, ispod polja za email.
object AuthValidator {

    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    data class RegisterErrors(
        @StringRes val name: Int? = null,
        @StringRes val email: Int? = null,
        @StringRes val password: Int? = null,
        @StringRes val confirmPassword: Int? = null
    ) {
        val isValid: Boolean get() = name == null && email == null && password == null && confirmPassword == null
    }

    // VR-01 do VR-05.
    fun validateRegister(name: String, email: String, password: String, confirmPassword: String): RegisterErrors =
        RegisterErrors(
            name = if (name.trim().length !in 2..100) R.string.error_vr_name else null,
            email = if (!isValidEmail(email)) R.string.error_vr_email else null,
            password = if (password.length < 8) R.string.error_vr_password else null,
            confirmPassword = if (confirmPassword != password) R.string.error_vr_confirm_password else null
        )

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && email.length <= 255 && EMAIL_REGEX.matches(email)
}
