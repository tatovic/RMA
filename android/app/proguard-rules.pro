# Pravila za R8 u release build-u (tiket 28, nalaz C9).
#
# Do tiketa 28 je `isMinifyEnabled` bio false, pa je release APK isporucivao pun, neskracen i
# neobfuskovan kod. Ukljucivanjem R8 svaka biblioteka koja se oslanja na refleksiju mora dobiti
# izuzece — inace se kod NE RUSI pri prevodjenju nego tek na uredjaju, i to tiho: Gson naprosto
# ostavi polje null jer vise ne postoji ime po kojem bi ga nasao.

# ---------------------------------------------------------------
# Gson + Retrofit DTO-i
# ---------------------------------------------------------------
# NAJVAZNIJE PRAVILO OVDE. Gson mapira JSON na polja po IMENU polja u klasi. R8 preimenuje polja u
# a, b, c — i tada `@SerializedName("purchasePrice")` vise nema sta da popuni. Zato se svi DTO-i
# cuvaju u celosti, sa originalnim imenima polja.
-keep class rs.homeinventory.app.data.remote.dto.** { *; }

# Isto vazi i van tog paketa. Ovo pravilo je dodato tek posto je release build STVARNO pokrenut na
# uredjaju: `JwtUtils.JwtPayloadDto` je privatna ugnjezdena klasa u `util/`, pa je gornje pravilo nije
# pokrivalo. R8 joj je preimenovao polje `exp`, Gson ga vise nije nalazio i vracao null, pa je
# `hasValidSession()` uvek bio false — prijava bi na serveru uspela (200), a aplikacija bi se odmah
# vracala na ekran Prijave, u petlju. Nijedan izuzetak, nijedna poruka, nista u logu. Tacno taj tip
# tihog kvara zbog kojeg se release build mora proveriti na uredjaju, a ne samo prevesti.
#
# Cuva imena SVIH polja obelezenih @SerializedName, gde god se klasa nalazila.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# I eksplicitno, jer se pravilo iznad pokazalo nedovoljnim: R8 sme da ugnjezdenu `data` klasu koja se
# koristi na jednom mestu ukloni ili ugradi u pozivaoca pre nego sto uopste stigne do poklapanja po
# anotaciji. Ovo je klasa cije je preimenovanje polja obaralo prijavu u release build-u — drzi se
# imenom, bez oslanjanja na to kako R8 rasporedjuje optimizacije.
-keep class rs.homeinventory.app.util.JwtUtils$JwtPayloadDto { *; }

# Anotacije i generici moraju prezivati: Gson bez Signature atributa ne ume da razlikuje
# List<ItemDto> od sirove List.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Gson-ovi interni tipovi.
-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit: interfejsi servisa se citaju refleksijom, kao i anotacije metoda i parametara.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <1>
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ---------------------------------------------------------------
# Room
# ---------------------------------------------------------------
# Room generise implementacije u vreme prevodjenja (KSP), pa mu treba manje od Gson-a, ali entiteti
# i projekcije se i dalje popunjavaju po imenima kolona.
-keep class rs.homeinventory.app.data.local.entity.** { *; }
-keep class rs.homeinventory.app.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# TypeConverter-i i enum-ovi koje oni prevode: enum vrednosti se u bazi cuvaju kao NAZIV konstante
# (Converters.kt), pa preimenovanje konstante znaci da se postojeca baza vise ne cita.
-keepclassmembers enum rs.homeinventory.app.data.local.** { *; }

# ---------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * { @javax.inject.Inject <init>(...); }

# ---------------------------------------------------------------
# MPAndroidChart (tiket 23)
# ---------------------------------------------------------------
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ---------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------
# Navigation Safe Args / fragmenti iz XML-a
# ---------------------------------------------------------------
# Fragmenti se instanciraju po imenu klase iz navigacionih grafova, ne pozivom konstruktora.
-keep class * extends androidx.fragment.app.Fragment { <init>(); }
