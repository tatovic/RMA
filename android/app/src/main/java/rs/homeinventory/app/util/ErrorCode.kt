package rs.homeinventory.app.util

// Katalog kodova gresaka — prd.md sekcija 10.
enum class ErrorCode {
    VALIDATION_ERROR,
    INVALID_CREDENTIALS,
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    ACCOUNT_DEACTIVATED,
    FORBIDDEN,
    NOT_FOUND,
    EMAIL_ALREADY_EXISTS,
    CATEGORY_NAME_TAKEN,
    LOCATION_NAME_TAKEN,
    CATEGORY_IN_USE,
    LOCATION_IN_USE,
    CANNOT_DEACTIVATE_SELF,
    SYNC_CONFLICT,
    WRONG_CURRENT_PASSWORD,
    INTERNAL_ERROR,

    // Greske koje ne stizu sa servera — prd.md sekcija 10, "Greske koje nastaju na klijentu".
    NO_NETWORK,
    TIMEOUT,
    SERVER_UNAVAILABLE,
    CURRENCY_UNAVAILABLE,
    UNKNOWN;

    companion object {
        // ERR-02 — nepoznat/nedokumentovan kod se nikada ne prikazuje sirov, uvek pada na UNKNOWN.
        fun fromServerCode(code: String?): ErrorCode = entries.find { it.name == code } ?: UNKNOWN
    }
}
