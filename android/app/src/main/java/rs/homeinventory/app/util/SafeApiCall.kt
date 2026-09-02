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
    val errorBody = rawBody?.let {
        runCatching { errorBodyGson.fromJson(it, ErrorResponseDto::class.java).error }.getOrNull()
    }
    val errorCode = ErrorCode.fromServerCode(errorBody?.code)
    val message = when (errorCode) {
        // CATEGORY_IN_USE/LOCATION_IN_USE nose broj predmeta u details.itemCount (backend/src/utils/errorCodes.js);
        // 0 je bezbedan fallback ako telo greske ne moze da se parsira (ERR-02/NFR-11, poruka se ne rusi).
        ErrorCode.CATEGORY_IN_USE, ErrorCode.LOCATION_IN_USE -> {
            val itemCount = runCatching {
                errorBody?.details?.asJsonObject?.get("itemCount")?.asInt
            }.getOrNull() ?: 0
            errorMessageProvider.message(errorCode, itemCount)
        }
        // prd.md sekcija 10 — VALIDATION_ERROR "se prikazuje po poljima iz details" (validate.js salje
        // vec lokalizovanu poruku po polju). Klijentski validatori (VR-01..VR-20) hvataju skoro sve pre
        // slanja; ovo je odbrambeni fallback za ono sto oni ne pokriju identicno kao server.
        ErrorCode.VALIDATION_ERROR -> {
            val fieldMessages = runCatching {
                errorBody?.details?.asJsonArray?.mapNotNull { entry ->
                    entry.asJsonObject.get("message")?.asString
                }
            }.getOrNull()?.filter { it.isNotBlank() }
            if (fieldMessages.isNullOrEmpty()) {
                errorMessageProvider.message(errorCode)
            } else {
                fieldMessages.joinToString(separator = "\n")
            }
        }
        else -> errorMessageProvider.message(errorCode)
    }
    return Resource.Error(errorCode, message)
}
