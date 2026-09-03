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

    // `@field:` je ovde obavezan, ne stilski izbor (tiket 28, nalaz C9). Bez prefiksa Kotlin
    // anotaciju stavlja SAMO na parametar konstruktora, pa je Gson na polju nikad ne vidi i tiho pada
    // na poklapanje po IMENU polja. To radi dok imena postoje — a u release build-u sa R8 ne postoje:
    // `exp` postane `a`, Gson vrati null, `hasValidSession()` uvek kaze false i prijava upada u
    // petlju (server vrati 200, aplikacija se odmah vrati na ekran Prijave). Sa `@field:` anotacija
    // stize do polja, pa je hvata i pravilo u proguard-rules.pro koje cuva sva @SerializedName polja.
    private data class JwtPayloadDto(@field:SerializedName("exp") val exp: Long?)
}
