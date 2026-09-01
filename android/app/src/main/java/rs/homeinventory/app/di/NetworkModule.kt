package rs.homeinventory.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rs.homeinventory.app.BuildConfig
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.api.CurrencyApi
import rs.homeinventory.app.data.remote.interceptor.AuthInterceptor
import rs.homeinventory.app.data.session.SessionManager
import rs.homeinventory.app.util.AndroidErrorMessageProvider
import rs.homeinventory.app.util.ErrorMessageProvider
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

// tech.md sekcija 9. Dva odvojena OkHttp klijenta su obavezna — slanje naseg JWT tokena
// na open.er-api.com bilo bi curenje kredencijala ka trecoj strani.
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun authInterceptor(prefs: UserPreferences, sessionManager: SessionManager): AuthInterceptor =
        AuthInterceptor(prefs, sessionManager)

    @Provides
    @Singleton
    fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        // Detaljno logovanje mreznog saobracaja iskljucivo u debug build-u.
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    @Provides
    @Singleton
    @Named("backend")
    fun backendClient(auth: AuthInterceptor, log: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(log)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    // BEZ AuthInterceptor-a.
    @Provides
    @Singleton
    @Named("currency")
    fun currencyClient(log: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(log)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun backendApi(@Named("backend") client: OkHttpClient): BackendApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)

    @Provides
    @Singleton
    fun currencyApi(@Named("currency") client: OkHttpClient): CurrencyApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.CURRENCY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)

    @Provides
    @Singleton
    fun errorMessageProvider(@ApplicationContext context: Context): ErrorMessageProvider =
        AndroidErrorMessageProvider(context)
}
