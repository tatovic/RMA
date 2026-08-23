package rs.homeinventory.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rs.homeinventory.app.R

class AuthValidatorTest {

    @Test
    fun `ispravni podaci prolaze bez gresaka`() {
        val errors = AuthValidator.validateRegister("Marko", "marko@primer.rs", "lozinka123", "lozinka123")

        assertTrue(errors.isValid)
    }

    @Test
    fun `ime krace od 2 znaka daje VR-01`() {
        val errors = AuthValidator.validateRegister("M", "marko@primer.rs", "lozinka123", "lozinka123")

        assertEquals(R.string.error_vr_name, errors.name)
    }

    @Test
    fun `neispravan format email-a daje VR-02`() {
        val errors = AuthValidator.validateRegister("Marko", "nije-email", "lozinka123", "lozinka123")

        assertEquals(R.string.error_vr_email, errors.email)
    }

    @Test
    fun `lozinka kraca od 8 znakova daje VR-04`() {
        val errors = AuthValidator.validateRegister("Marko", "marko@primer.rs", "kratka", "kratka")

        assertEquals(R.string.error_vr_password, errors.password)
    }

    @Test
    fun `nepoklapajuce lozinke daju VR-05`() {
        val errors = AuthValidator.validateRegister("Marko", "marko@primer.rs", "lozinka123", "drugacije123")

        assertEquals(R.string.error_vr_confirm_password, errors.confirmPassword)
    }

    @Test
    fun `granicna duzina imena od 2 znaka je validna`() {
        val errors = AuthValidator.validateRegister("Ma", "marko@primer.rs", "lozinka123", "lozinka123")

        assertNull(errors.name)
    }
}
