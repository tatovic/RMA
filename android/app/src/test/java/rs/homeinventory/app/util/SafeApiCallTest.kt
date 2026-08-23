package rs.homeinventory.app.util

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeApiCallTest {

    // Vraca kod kao poruku radi jednostavne provere u testovima (ERR-02 se testira odvojeno kroz AndroidErrorMessageProvider).
    private val messages = ErrorMessageProvider { code -> code.name }

    private fun errorBody(json: String) = json.toResponseBody("application/json".toMediaType())

    @Test
    fun `uspesan odgovor sa telom vraca Resource Success`() = runTest {
        val result = safeApiCall(messages) { Response.success("podaci") }

        assertEquals(Resource.Success("podaci"), result)
    }

    @Test
    fun `uspesan odgovor bez tela (204) vraca Resource Success Unit`() = runTest {
        val result = safeApiCall<Unit>(messages) { Response.success(null) }

        assertTrue(result is Resource.Success)
    }

    @Test
    fun `telo greske sa poznatim kodom se prevodi u odgovarajuci ErrorCode`() = runTest {
        val body = errorBody("""{"error":{"code":"NOT_FOUND","message":"Traženi podatak ne postoji."}}""")

        val result = safeApiCall<String>(messages) { Response.error(404, body) }

        assertEquals(Resource.Error(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.name), result)
    }

    @Test
    fun `401 sa TOKEN_EXPIRED kodom se ne generalizuje u TOKEN_INVALID`() = runTest {
        val body = errorBody("""{"error":{"code":"TOKEN_EXPIRED","message":"Sesija je istekla."}}""")

        val result = safeApiCall<String>(messages) { Response.error(401, body) }

        assertEquals(ErrorCode.TOKEN_EXPIRED, (result as Resource.Error).code)
    }

    @Test
    fun `401 sa INVALID_CREDENTIALS kodom (neuspela prijava) se ne generalizuje`() = runTest {
        val body = errorBody("""{"error":{"code":"INVALID_CREDENTIALS","message":"Pogrešan email ili lozinka"}}""")

        val result = safeApiCall<String>(messages) { Response.error(401, body) }

        assertEquals(ErrorCode.INVALID_CREDENTIALS, (result as Resource.Error).code)
    }

    @Test
    fun `nepoznat kod greske sa servera pada na UNKNOWN (ERR-02)`() = runTest {
        val body = errorBody("""{"error":{"code":"NESTO_NOVO_STO_KLIJENT_NE_POZNAJE","message":"…"}}""")

        val result = safeApiCall<String>(messages) { Response.error(400, body) }

        assertEquals(ErrorCode.UNKNOWN, (result as Resource.Error).code)
    }

    @Test
    fun `neparsljivo telo greske pada na UNKNOWN umesto da baci izuzetak`() = runTest {
        val body = errorBody("nije JSON")

        val result = safeApiCall<String>(messages) { Response.error(500, body) }

        assertEquals(ErrorCode.UNKNOWN, (result as Resource.Error).code)
    }

    @Test
    fun `UnknownHostException postaje NO_NETWORK`() = runTest {
        val result = safeApiCall<String>(messages) { throw UnknownHostException() }

        assertEquals(Resource.Error(ErrorCode.NO_NETWORK, ErrorCode.NO_NETWORK.name), result)
    }

    @Test
    fun `SocketTimeoutException postaje TIMEOUT`() = runTest {
        val result = safeApiCall<String>(messages) { throw SocketTimeoutException() }

        assertEquals(Resource.Error(ErrorCode.TIMEOUT, ErrorCode.TIMEOUT.name), result)
    }

    @Test
    fun `ostale IOException postaju SERVER_UNAVAILABLE`() = runTest {
        val result = safeApiCall<String>(messages) { throw IOException("connection refused") }

        assertEquals(Resource.Error(ErrorCode.SERVER_UNAVAILABLE, ErrorCode.SERVER_UNAVAILABLE.name), result)
    }

    @Test
    fun `neocekivan izuzetak postaje UNKNOWN i ne prosledjuje svoju poruku (ERR-03)`() = runTest {
        val result = safeApiCall<String>(messages) { throw IllegalStateException("interna tajna poruka") }

        assertEquals(Resource.Error(ErrorCode.UNKNOWN, ErrorCode.UNKNOWN.name), result)
        assertTrue((result as Resource.Error).message != "interna tajna poruka")
    }
}
