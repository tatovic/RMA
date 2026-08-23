package rs.homeinventory.app.util

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JwtUtilsTest {

    private fun fakeToken(payloadJson: String?): String {
        val header = encode("""{"alg":"HS256","typ":"JWT"}""")
        val payload = payloadJson?.let { encode(it) } ?: "not-base64!!"
        return "$header.$payload.signature"
    }

    private fun encode(json: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

    @Test
    fun `cita exp polje i pretvara ga u milisekunde`() {
        val token = fakeToken("""{"sub":"1","role":"USER","iat":1000,"exp":2000}""")

        assertEquals(2000000L, JwtUtils.expirationMillis(token))
    }

    @Test
    fun `nedostajuce polje exp vraca null`() {
        val token = fakeToken("""{"sub":"1","role":"USER","iat":1000}""")

        assertNull(JwtUtils.expirationMillis(token))
    }

    @Test
    fun `token bez tacaka vraca null`() {
        assertNull(JwtUtils.expirationMillis("nije-jwt"))
    }

    @Test
    fun `neparsljiv payload vraca null umesto da baci izuzetak`() {
        assertNull(JwtUtils.expirationMillis(fakeToken(null)))
    }
}
