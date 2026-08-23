package rs.homeinventory.app.util

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import rs.homeinventory.app.R
import javax.inject.Inject

// ERR-02 — korisniku se prikazuje iskljucivo tekst iz strings.xml, mapiran preko ErrorCode.
class AndroidErrorMessageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ErrorMessageProvider {

    override fun message(code: ErrorCode): String = context.getString(resFor(code))

    @StringRes
    private fun resFor(code: ErrorCode): Int = when (code) {
        ErrorCode.VALIDATION_ERROR -> R.string.error_validation
        ErrorCode.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
        ErrorCode.TOKEN_EXPIRED -> R.string.error_token_expired
        ErrorCode.TOKEN_INVALID -> R.string.error_token_invalid
        ErrorCode.ACCOUNT_DEACTIVATED -> R.string.error_account_deactivated
        ErrorCode.FORBIDDEN -> R.string.error_forbidden
        ErrorCode.NOT_FOUND -> R.string.error_not_found
        ErrorCode.EMAIL_ALREADY_EXISTS -> R.string.error_email_already_exists
        ErrorCode.CATEGORY_NAME_TAKEN -> R.string.error_category_name_taken
        ErrorCode.LOCATION_NAME_TAKEN -> R.string.error_location_name_taken
        ErrorCode.CATEGORY_IN_USE -> R.string.error_category_in_use
        ErrorCode.LOCATION_IN_USE -> R.string.error_location_in_use
        ErrorCode.CANNOT_DEACTIVATE_SELF -> R.string.error_cannot_deactivate_self
        ErrorCode.SYNC_CONFLICT -> R.string.error_sync_conflict
        ErrorCode.WRONG_CURRENT_PASSWORD -> R.string.error_wrong_current_password
        ErrorCode.INTERNAL_ERROR -> R.string.error_internal
        ErrorCode.NO_NETWORK -> R.string.error_no_network
        ErrorCode.TIMEOUT, ErrorCode.SERVER_UNAVAILABLE -> R.string.error_server_unavailable
        ErrorCode.CURRENCY_UNAVAILABLE -> R.string.error_currency_unavailable
        ErrorCode.UNKNOWN -> R.string.error_unknown
    }
}
