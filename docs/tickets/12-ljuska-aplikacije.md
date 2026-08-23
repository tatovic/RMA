# 12: Ljuska aplikacije i navigacija

**What to build:** Aplikacija dobija svoj stalni oblik: donja navigacija sa četiri odeljka kroz koje korisnik slobodno šeta, i odjava koja ga čisto izbacuje napolje. Ekrani su još prazni, ali kostur navigacije kroz koji će sve naredno da se ukloni je gotov.

**Blocked by:** 11 — Prijava i registracija na uređaju.

**Status:** done

- [x] Glavna aktivnost sadrži donju navigaciju sa četiri odeljka: pregled, inventar, statistika i profil — `ui/MainActivity.kt` (ACT-2), `activity_main.xml` (`BottomNavigationView` + `nav_main.xml`), `menu/bottom_nav_menu.xml`; uživo provereno prelaskom kroz sva četiri taba na emulatoru
- [x] Postoje sva četiri navigaciona grafa iz tech.md sekcija 10, po jedan za svaku aktivnost — `nav_auth.xml` (iz tiketa 11), `nav_main.xml` (ACT-2, 6 destinacija), `nav_details.xml` (ACT-3) i `nav_admin.xml` (ACT-4); `ui/ItemDetailsActivity.kt` i `ui/AdminActivity.kt` su minimalne ljuske koje ih hostuju — sadržaj i ulazne tačke dolaze u tiketima 16 i 25
- [x] Prelazak između odeljaka čuva stanje svakog od njih — `BottomNavigationView.setupWithNavController()` (NavigationUI podrazumevano radi `saveState`/`restoreState` po odeljku)
- [x] Donja navigacija je identična za obe role; ništa se u njoj ne skriva ni ne dodaje (SCR-09) — `bottom_nav_menu.xml` je statičan, bez uslovne logike po roli
- [x] Ekrani do kojih se ne dolazi iz menija sakrivaju donju navigaciju dok su otvoreni — `MainActivity.setupNavigation()` prati `addOnDestinationChangedListener` i sakriva bar van četiri glavne destinacije; uživo provereno za SCR-06 (FAB u `InventoryFragment`) i SCR-10 (dugme u `ProfileFragment`)
- [x] Profil prikazuje ime, email i rolu prijavljenog korisnika — `presentation/profile/ProfileFragment.kt` + `ProfileViewModel.kt`, čita `AuthRepository.currentUser`; uživo provereno (ime, email i rola "Administrator" prikazani tačno)
- [x] Odjava traži potvrdu kroz dijalog (BR-008) — `MaterialAlertDialogBuilder` u `ProfileFragment`; uživo provereno
- [x] Odjava briše token i kompletan sadržaj lokalne baze, pa vraća na ekran prijave (BR-005) — `AuthRepository.logout()` (`UserPreferences.clearSession()` + `HomeInventoryDatabase.clearAllData()`); uživo provereno preko `sqlite3` upita nad `home_inventory.db` (svih 0 redova u `users`/`categories`/`locations`/`inventory_items`) i preko sadržaja `user_prefs.preferences_pb` (0 bajtova posle odjave)
- [x] Posle odjave povratno dugme ne vraća u aplikaciju — `Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` + `requireActivity().finish()`; uživo provereno (povratno dugme posle odjave ide na launcher, ne u aplikaciju)
- [x] Rotacija ekrana ne ruši navigaciju i ne gubi izabrani odeljak — uživo provereno rotacijom emulatora u pejzažni mod na SCR-04 (Inventar ostaje izabran tab, sadržaj i FAB ostaju ispravno pozicionirani)

**Napomena:** Uživo provereno na `Pixel6_API36` emulatoru + lokalnom backend/MySQL (`npm run dev`, baza vraćena u čisto stanje sa `npm run db:reset` posle testa). Registracija novog naloga (prvi korisnik u bazi → ADMIN po BR-001), prelazak kroz sva četiri bottom nav taba, navigacija ka SCR-06 i SCR-10 sa sakrivanjem bara, rotacija ekrana, prikaz profila i kompletan tok odjave (dijalog → brisanje tokena i baze → ekran prijave → povratno dugme ne vraća nazad) — sve potvrđeno na uređaju.

Usput otkriven i ispravljen layout bug: statička `marginBottom="?attr/actionBarSize"` na `NavHostFragment`-u nije odgovarala stvarnoj (višoj) visini `BottomNavigationView`-a sa M3 labelama, pa je FAB na SCR-04 bio delimično sakriven iza bara. Ispravljeno merenjem stvarne visine bara u `MainActivity.setContentBottomMargin()` (`bottomNavigation.doOnLayout { ... }`) umesto pretpostavljene konstante.

Ostali ekrani (SCR-03, 05, 06, 07, 08, 10, 11, 12, 13) su namerno prazni placeholderi (`fragment_placeholder.xml`) — sadržaj dolazi u tiketima 13, 14, 15, 16, 17, 23, 25. `gradlew :app:testDebugUnitTest` i `gradlew :app:assembleDebug` BUILD SUCCESSFUL.
