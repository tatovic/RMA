package rs.homeinventory.app.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Base64

// FR-011 — cita "exp" iz JWT payload-a bez mreznog poziva, da bi se pri pokretanju aplikacije
// izabrao pocetni ekran. Potpis se namerno ne verifikuje ovde — to je iskljucivo posao servera
// (SEC-02); klijent ovim samo bira UI, server i dalje odbija istekao/neispravan token na svakom zahtevu.
object JwtUtils {
    private val gson = Gson()

    fun expirationMillis(token: String): Long? {
        val payload = token.split(".").getOrNull(1) ?: return null
        val json = runCatching { String(Base64.getUrlDecoder().decode(padBase64Url(payload))) }.getOrNull()
            ?: return null
        val exp = runCatching { gson.fromJson(json, JwtPayloadDto::class.java)?.exp }.getOrNull() ?: return null
        return exp * 1000
    }

    private fun padBase64Url(value: String): String {
        val remainder = value.length % 4
        return if (remainder == 0) value else value + "=".repeat(4 - remainder)
    }

    private data class JwtPayloadDto(@SerializedName("exp") val exp: Long?)
}
