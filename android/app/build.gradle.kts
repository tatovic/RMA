import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

android {
    namespace = "rs.homeinventory.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "rs.homeinventory.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Adrese dolaze iz build konfiguracije, ne iz koda, vidi tech.md sekcija 4
        buildConfigField("String", "BACKEND_BASE_URL", "\"http://10.0.2.2:3000/api/\"")
        buildConfigField("String", "CURRENCY_BASE_URL", "\"https://open.er-api.com/v6/\"")
    }

    buildTypes {
        release {
            // Tiket 28 (nalaz C9) — release build je do sada isporucivao pun, neskracen i
            // neobfuskovan kod. Pravila koja cuvaju Gson DTO-e, Room i Hilt su u proguard-rules.pro;
            // najveci rizik su DTO-i, jer Gson trazi polja po imenu i posle preimenovanja tiho
            // ostavlja null umesto da pukne. Zato je release build obavezno proveriti na uredjaju.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // android.util.Log nije mokovan u JUnit testovima bez Robolectric-a; SafeApiCall ga koristi za ERR-04.
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        // MigrationTestHelper cita izvezene JSON seme kao asset (tiket 27, vidi MigrationTest).
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    // Room exportSchema, vidi tech.md sekcija 14 (schemas/ se ne ignorise u Gitu)
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // FR-082 — EXIF orijentacija fotografije sa kamere pre kompresije.
    implementation(libs.exifinterface)

    implementation(libs.mpandroidchart)

    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
