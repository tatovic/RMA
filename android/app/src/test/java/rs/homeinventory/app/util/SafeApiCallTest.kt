package rs.homeinventory.app.util

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeApiCallTest {

    // Vraca kod (i eventualne format argumente) kao poruku radi jednostavne provere u testovima
    // (ERR-02 se testira odvojeno kroz AndroidErrorMessageProvider).
    private val messages = ErrorMessageProvider { code, formatArgs ->
        if (formatArgs.isEmpty()) code.name else "${code.name}:${formatArgs.joinToString(",")}"
    }

    private fun errorBody(json: String) = json.toResponseBody("application/json".toMediaType())

    // Uspesan odgovor BEZ tela sa zadatim statusnim kodom. Retrofit-ov Response.success(code, body)
    // je ovde dvosmislen za Kotlin (sudara se sa success(body, headers)), pa se sirovi OkHttp odgovor
    // gradi eksplicitno.
    private fun <T> emptyBody(code: Int): Response<T> = Response.success(
        null,
        okhttp3.Response.Builder()
            .code(code)
            .message("No Content")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
            .build()
    )

    @Test
    fun `uspesan odgovor sa telom vraca Resource Success`() = runTest {
        val result = safeApiCall(messages) { Response.success("podaci") }

        assertEquals(Resource.Success("podaci"), result)
    }

    @Test
    fun `uspesan odgovor bez tela (204) vraca Resource Success Unit`() = runTest {
        // Test je ranije gradio Response.success(null), sto je 200 bez tela, a ne 204 — naziv testa
        // je tvrdio jedno, a provera radila drugo. Od tiketa 28 (nalaz C5) ta razlika je bitna, pa se
        // ovde pravi stvaran 204.
        val result = safeApiCall<Unit>(messages) { emptyBody(204) }

        assertTrue(result is Resource.Success)
    }

    // Nalaz C5 — `Unit as T` je tacan samo za 204. Svaki drugi 2xx bez tela znaci da je poziv ocekivao
    // DTO i dobio prazno; ranije je i on prolazio kao uspeh, pa bi se pozivalac srusio tek pri prvom
    // pristupu polju, daleko od mesta greske.
    @Test
    fun `uspesan odgovor bez tela koji nije 204 se tretira kao greska`() = runTest {
        val result = safeApiCall<String>(messages) { emptyBody(200) }

        assertEquals(Resource.Error(ErrorCode.UNKNOWN, ErrorCode.UNKNOWN.name), result)
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
    fun `CATEGORY_IN_USE prosledjuje broj predmeta iz details polja (prd md sekcija 10, {n} predmeta)`() = runTest {
        val body = errorBody(
            """{"error":{"code":"CATEGORY_IN_USE","message":"…","details":{"itemCount":7}}}"""
        )

        val result = safeApiCall<String>(messages) { Response.error(409, body) }

        assertEquals(Resource.Error(ErrorCode.CATEGORY_IN_USE, "CATEGORY_IN_USE:7"), result)
    }

    @Test
    fun `CATEGORY_IN_USE bez citljivog details polja pada na 0 umesto da baci izuzetak`() = runTest {
        val body = errorBody("""{"error":{"code":"CATEGORY_IN_USE","message":"…"}}""")

        val result = safeApiCall<String>(messages) { Response.error(409, body) }

        assertEquals(Resource.Error(ErrorCode.CATEGORY_IN_USE, "CATEGORY_IN_USE:0"), result)
    }

    @Test
    fun `VALIDATION_ERROR prikazuje poruke po poljima iz details (prd md sekcija 10)`() = runTest {
        val body = errorBody(
            """{"error":{"code":"VALIDATION_ERROR","message":"Neispravni podaci","details":[
                {"field":"name","message":"Naziv je obavezan"},
                {"field":"categoryId","message":"Kategorija je obavezna"}
            ]}}"""
        )

        val result = safeApiCall<String>(messages) { Response.error(400, body) }

        assertEquals(
            Resource.Error(ErrorCode.VALIDATION_ERROR, "Naziv je obavezan\nKategorija je obavezna"),
            result
        )
    }

    @Test
    fun `VALIDATION_ERROR bez citljivog details polja pada na generalnu poruku iz kataloga`() = runTest {
        val body = errorBody("""{"error":{"code":"VALIDATION_ERROR","message":"Neispravni podaci"}}""")

        val result = safeApiCall<String>(messages) { Response.error(400, body) }

        assertEquals(Resource.Error(ErrorCode.VALIDATION_ERROR, "VALIDATION_ERROR"), result)
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
