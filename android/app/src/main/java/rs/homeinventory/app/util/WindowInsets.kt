package rs.homeinventory.app.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// NFR-10 (tiket 28, nalaz 14) — od targetSdk 35 pa nadalje sistem crta aplikaciju preko celog ekrana
// i vise ne postoji nacin da se to iskljuci. Bez ovoga gornji red svakog ekrana zavrsava ispod
// statusne trake, a donji ispod trake za navigaciju/gestove. Ne pogadja jedan uredjaj nego SVAKI.
//
// Razmak se dodaje kao padding korenu aktivnosti, pa vazi za sve fragmente u njoj. `android:fitsSystemWindows`
// se namerno ne koristi — na CoordinatorLayout-u se ponasa drugacije nego na ConstraintLayout-u, a
// ovako je pravilo isto na sva cetiri ekrana.
fun View.applySystemBarsPadding() {
    // Pocetni padding se pamti da ponovljena primena (rotacija, promena teme) ne bi gomilala razmak.
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = initialLeft + bars.left,
            top = initialTop + bars.top,
            right = initialRight + bars.right,
            bottom = initialBottom + bars.bottom
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}
