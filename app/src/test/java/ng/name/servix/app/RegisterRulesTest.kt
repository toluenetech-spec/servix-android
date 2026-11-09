package ng.name.servix.app

import ng.name.servix.app.ui.auth.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterRulesTest {
    @Test fun checklistStartsEmptyAndHasFiveRules() {
        val state = AuthUiState()
        assertEquals(5, state.passwordRules.size)
        assertEquals(0, state.passwordRules.count { it.met })
        assertFalse(state.canSubmitRegistration)
    }

    @Test fun submissionRequiresEveryRuleMatchingConfirmationAndTerms() {
        val good = AuthUiState(registerPassword = "Servix1!", registerConfirmPassword = "Servix1!", registerTermsAccepted = true)
        assertTrue(good.passwordRules.all { it.met })
        assertTrue(good.passwordsMatch)
        assertTrue(good.canSubmitRegistration)
        assertFalse(good.copy(registerTermsAccepted = false).canSubmitRegistration)
        assertFalse(good.copy(registerConfirmPassword = "different").canSubmitRegistration)
        assertFalse(good.copy(registerPassword = "Servix12", registerConfirmPassword = "Servix12").canSubmitRegistration)
        assertFalse(good.copy(registerSubmitting = true).canSubmitRegistration)
        assertFalse(good.copy(registerPassword = "A1!" + "a".repeat(198), registerConfirmPassword = "A1!" + "a".repeat(198)).canSubmitRegistration)
    }

    @Test fun registrationDoesNotAlterSecurityResetPolicy() {
        assertEquals(6, AuthUiState.evaluatePasswordRules("Servix1!", null).size)
        assertEquals(2, AuthUiState.evaluatePasswordRules("Servix1!", "basic").size)
    }
}
