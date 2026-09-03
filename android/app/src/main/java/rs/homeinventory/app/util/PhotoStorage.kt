package rs.homeinventory.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// FR-082/FR-083 — fotografije predmeta se cuvaju iskljucivo u internom skladistu aplikacije, smanjene
// na najvise MAX_DIMENSION_PX duze stranice i kompresovane; u bazu ide samo naziv fajla (DB-RULE-02).
private const val TAG = "PhotoStorage"
private const val PHOTOS_DIR_NAME = "photos"
// Mora se poklapati sa <cache-path name="captured_photos" .../> u res/xml/file_paths.xml.
private const val CAPTURE_DIR_NAME = "captured_photos"
private const val MAX_DIMENSION_PX = 1080
private const val JPEG_QUALITY = 80

// Koristi se i van PhotoStorage-a (adapter/detalji) za prikaz vec sacuvane fotografije, bez DI.
fun photoFile(context: Context, fileName: String): File =
    File(File(context.filesDir, PHOTOS_DIR_NAME), fileName)

@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Ucitava sliku sa datog Uri (galerija ili privremeni snimak kamere), smanjuje je i kompresuje
    // (FR-082) i kopira u privatni prostor aplikacije (FR-083). Vraca naziv sacuvanog fajla, ili null
    // ako slika nije mogla da se ucita (npr. osteceni ili neocekivani sadrzaj).
    fun save(sourceUri: Uri): String? {
        // I samo citanje ume da padne: Uri iz galerije kome je dozvola u medjuvremenu istekla baca
        // SecurityException, a prekinut stream IOException. Oba su do sada izlazila iz save() uprkos
        // dokumentovanom "vraca null" ugovoru (tiket 28, nalaz C4).
        val bitmap = try {
            decodeScaledBitmap(sourceUri)
        } catch (e: IOException) {
            Log.e(TAG, "Fotografija nije mogla da se ucita", e) // ERR-04
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Nema dozvole za citanje odabrane fotografije", e) // ERR-04
            null
        } ?: return null

        val fileName = "${UUID.randomUUID()}.jpg"
        val destination = photoFile(context, fileName)
        try {
            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        } catch (e: IOException) {
            // Ugovor iz dokumentacije iznad kaze "vraca null ako slika nije mogla da se ucita", ali
            // je vazio samo za dekodiranje — pun disk ili greska pri upisu su izlazili kao izuzetak
            // van jedinog mesta koje sme da hvata (ERR-01), pravo u viewModelScope (tiket 28, nalaz C4).
            Log.e(TAG, "Fotografija nije mogla da se upise", e) // ERR-04
            destination.delete() // polovicno upisan fajl ne sme da ostane
            return null
        } catch (e: SecurityException) {
            Log.e(TAG, "Nema dozvole za citanje odabrane fotografije", e) // ERR-04
            destination.delete()
            return null
        } finally {
            bitmap.recycle()
        }

        // FR-083 — original iz kesa (snimak kamere) je od ovog trenutka suvisan: sacuvana je smanjena
        // i kompresovana kopija u privatnom prostoru aplikacije. Bez ovoga je svaki snimak ostajao
        // dvaput na disku, u punoj velicini, dok sistem ne odluci da ocisti kes.
        deleteCapturedOriginal(sourceUri)

        return fileName
    }

    // Brise samo fajlove iz naseg cacheDir/captured_photos — Uri iz galerije se nikada ne dira.
    private fun deleteCapturedOriginal(sourceUri: Uri) {
        val fileName = sourceUri.lastPathSegment?.substringAfterLast('/') ?: return
        val captured = File(File(context.cacheDir, CAPTURE_DIR_NAME), fileName)
        if (captured.exists()) {
            runCatching { captured.delete() }
                .onFailure { Log.w(TAG, "Privremeni snimak nije mogao da se obrise", it) } // ERR-04
        }
    }

    // FR-086 — brisanje predmeta (i zamena fotografije) briše i fajl na disku da se ne gomilaju.
    fun delete(fileName: String?) {
        if (fileName == null) return
        photoFile(context, fileName).delete()
    }

    // FR-081 — privremeni fajl u koji aplikacija kamere upisuje snimak, dele preko FileProvider-a.
    // Original se brise cim save() sacuva kompresovanu kopiju (vidi deleteCapturedOriginal).
    fun createCaptureUri(): Uri {
        val dir = File(context.cacheDir, CAPTURE_DIR_NAME).apply { mkdirs() }
        val file = File(dir, "capture_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val sampled = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null

        val rotationDegrees = readExifRotationDegrees(uri)
        val resized = resizeToMaxDimension(sampled)
        return if (rotationDegrees == 0) resized else rotate(resized, rotationDegrees)
    }

    private fun computeSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= MAX_DIMENSION_PX && height / (sampleSize * 2) >= MAX_DIMENSION_PX) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun resizeToMaxDimension(bitmap: Bitmap): Bitmap {
        val longerSide = maxOf(bitmap.width, bitmap.height)
        if (longerSide <= MAX_DIMENSION_PX) return bitmap
        val scale = MAX_DIMENSION_PX.toFloat() / longerSide
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    // Kamera snima sliku uspravno rotiranu prema drzanju telefona; orijentacija se cuva u EXIF-u.
    private fun readExifRotationDegrees(uri: Uri): Int {
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) } ?: return 0
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}
