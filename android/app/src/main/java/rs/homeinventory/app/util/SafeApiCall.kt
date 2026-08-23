package rs.homeinventory.app.util

import android.util.Log
import com.google.gson.Gson
import retrofit2.Response
import rs.homeinventory.app.data.remote.dto.ErrorResponseDto
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "SafeApiCall"
private val errorBodyGson = Gson()

/**
 * Jedina funkcija koja izvrsava mrezni poziv i pretvara ishod u Resource.
 * ERR-01 — `catch (e: Exception)` postoji iskljucivo ovde.
 */
suspend fun <T> safeApiCall(
    errorMessageProvider: ErrorMessageProvider,
    call: suspend () -> Response<T>
): Resource<T> = try {
    val response = call()
    when {
        response.isSuccessful && response.body() != null -> Resource.Success(response.body()!!)
        // Uspesan odgovor bez tela (npr. 204 pri brisanju ili promeni lozinke).
        response.isSuccessful -> unitSuccess()
        else -> parseErrorBody(response, errorMessageProvider)
    }
} catch (e: UnknownHostException) {
    Log.e(TAG, "Nema mrezne konekcije", e) // ERR-04
    Resource.Error(ErrorCode.NO_NETWORK, errorMessageProvider.message(ErrorCode.NO_NETWORK))
} catch (e: SocketTimeoutException) {
    Log.e(TAG, "Isteklo je vreme cekanja na odgovor servera", e) // ERR-04
    Resource.Error(ErrorCode.TIMEOUT, errorMessageProvider.message(ErrorCode.TIMEOUT))
} catch (e: IOException) {
    Log.e(TAG, "Server nije dostupan", e) // ERR-04
    Resource.Error(ErrorCode.SERVER_UNAVAILABLE, errorMessageProvider.message(ErrorCode.SERVER_UNAVAILABLE))
} catch (e: Exception) {
    // ERR-03 — e.message se nikada ne prikazuje korisniku, samo se loguje (ERR-04).
    Log.e(TAG, "Neocekivana greska", e)
    Resource.Error(ErrorCode.UNKNOWN, errorMessageProvider.message(ErrorCode.UNKNOWN))
}

@Suppress("UNCHECKED_CAST")
private fun <T> unitSuccess(): Resource<T> = Resource.Success(Unit as T)

// Cita telo greske sa servera i prevodi njegov kod u poruku iz kataloga (prd.md sekcija 10).
private fun <T> parseErrorBody(response: Response<T>, errorMessageProvider: ErrorMessageProvider): Resource<T> {
    val rawBody = runCatching { response.errorBody()?.string() }.getOrNull()
    val serverCode = rawBody?.let {
        runCatching { errorBodyGson.fromJson(it, ErrorResponseDto::class.java).error.code }.getOrNull()
    }
    val errorCode = ErrorCode.fromServerCode(serverCode)
    return Resource.Error(errorCode, errorMessageProvider.message(errorCode))
}
