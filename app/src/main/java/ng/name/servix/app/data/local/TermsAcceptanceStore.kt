package ng.name.servix.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.termsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "servix_terms_acceptance"
)

class TermsAcceptanceStore(private val context: Context) {

    val acceptedTimestampMillis: Flow<Long?> = context.termsDataStore.data.map { prefs ->
        prefs[KEY_ACCEPTED_TIMESTAMP_MS]
    }

    val acceptedIsoTimestamp: Flow<String?> = context.termsDataStore.data.map { prefs ->
        prefs[KEY_ACCEPTED_ISO]
    }

    suspend fun recordTermsAccepted(timestampMillis: Long = System.currentTimeMillis()) {
        val iso = Instant.ofEpochMilli(timestampMillis).toString()
        context.termsDataStore.edit { prefs ->
            prefs[KEY_ACCEPTED_TIMESTAMP_MS] = timestampMillis
            prefs[KEY_ACCEPTED_ISO] = iso
        }
    }

    suspend fun getAcceptedTimestampMillis(): Long? = acceptedTimestampMillis.firstOrNull()

    suspend fun getAcceptedIsoTimestamp(): String? = acceptedIsoTimestamp.firstOrNull()

    companion object {
        private val KEY_ACCEPTED_TIMESTAMP_MS = longPreferencesKey("terms_accepted_timestamp_ms")
        private val KEY_ACCEPTED_ISO = stringPreferencesKey("terms_accepted_iso")
    }
}
