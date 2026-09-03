package rs.homeinventory.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

// Opseg koji zivi koliko i proces — za posao koji mora da se zavrsi i kada ekran koji ga je pokrenuo
// vise ne postoji. Uveden u tiketu 28 (nalaz 10) za ciscenje baze posle 401: taj posao ne sme da visi
// na viewModelScope-u koji nestaje cim se otvori ekran Prijave, a ni na OkHttp niti sa otvorenim
// odgovorom, gde je do sada stajao pod runBlocking-om.
//
// SupervisorJob — neuspeh jednog zadatka ne obara opseg za sve ostale.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
