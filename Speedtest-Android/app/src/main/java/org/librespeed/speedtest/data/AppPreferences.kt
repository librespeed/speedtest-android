package org.librespeed.speedtest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fdossena.speedtest.core.serverSelector.TestPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "settings")

//the public list carries a stable id; custom servers fall back to name+server
fun TestPoint.key(): String = if (serverId > 0) "id:$serverId" else "${name}|${server}"

class AppPreferences(private val context: Context) {

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorite_servers")
        val CUSTOM_SERVERS = stringPreferencesKey("custom_servers")
        val REMEMBERED_SERVER = stringPreferencesKey("remembered_server")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_MBYTES = booleanPreferencesKey("use_mbytes")
        val TELEMETRY = booleanPreferencesKey("telemetry_enabled")
        val SINGLE_CONNECTION = booleanPreferencesKey("single_connection")
        val TEST_MODE = stringPreferencesKey("test_mode")
        val ASKED_PHONE_STATE = booleanPreferencesKey("asked_phone_state")
        val SCHEDULED_TESTS = stringPreferencesKey("scheduled_tests")
        val GEO_CACHE = stringPreferencesKey("geo_cache")
    }

    suspend fun getGeoCache(): Map<String, Pair<Double, Double>> = try {
        val json = JSONObject(context.dataStore.data.first()[Keys.GEO_CACHE] ?: "{}")
        json.keys().asSequence().mapNotNull { name ->
            val parts = json.getString(name).split(",")
            parts.takeIf { it.size == 2 }?.let { name to (it[0].toDouble() to it[1].toDouble()) }
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }

    suspend fun putGeoCache(name: String, lat: Double, lon: Double) {
        context.dataStore.edit { preferences ->
            val json = try {
                JSONObject(preferences[Keys.GEO_CACHE] ?: "{}")
            } catch (_: Exception) {
                JSONObject()
            }
            json.put(name, "$lat,$lon")
            preferences[Keys.GEO_CACHE] = json.toString()
        }
    }

    val themeMode: Flow<String> =
        context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    val useMBytes: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.USE_MBYTES] ?: false }

    val telemetryEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.TELEMETRY] ?: false }

    /** "standard", "single" or "stability"; migrates the old single-connection switch. */
    val testMode: Flow<String> =
        context.dataStore.data.map {
            it[Keys.TEST_MODE] ?: if (it[Keys.SINGLE_CONNECTION] == true) "single" else "standard"
        }

    suspend fun setTestMode(value: String) {
        context.dataStore.edit { it[Keys.TEST_MODE] = value }
    }

    /** The one-time READ_PHONE_STATE prompt before a test on a mobile network. */
    val askedPhoneState: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ASKED_PHONE_STATE] ?: false }

    suspend fun markPhoneStateAsked() {
        context.dataStore.edit { it[Keys.ASKED_PHONE_STATE] = true }
    }

    /** "off", "6h", "daily" or "weekly" */
    val scheduledTests: Flow<String> =
        context.dataStore.data.map { it[Keys.SCHEDULED_TESTS] ?: "off" }

    suspend fun setScheduledTests(value: String) {
        context.dataStore.edit { it[Keys.SCHEDULED_TESTS] = value }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setUseMBytes(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_MBYTES] = value }
    }

    suspend fun setTelemetryEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.TELEMETRY] = value }
    }

    val favorites: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.FAVORITES] ?: emptySet() }

    val rememberedServer: Flow<String?> =
        context.dataStore.data.map { it[Keys.REMEMBERED_SERVER] }

    val customServers: Flow<List<TestPoint>> =
        context.dataStore.data.map { preferences ->
            parseCustomServers(preferences[Keys.CUSTOM_SERVERS] ?: "[]")
        }

    suspend fun toggleFavorite(key: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[Keys.FAVORITES] ?: emptySet()
            preferences[Keys.FAVORITES] = if (key in current) current - key else current + key
        }
    }

    suspend fun setRememberedServer(key: String?) {
        context.dataStore.edit { preferences ->
            if (key == null) preferences.remove(Keys.REMEMBERED_SERVER)
            else preferences[Keys.REMEMBERED_SERVER] = key
        }
    }

    suspend fun addCustomServer(testPoint: TestPoint) {
        context.dataStore.edit { preferences ->
            val list = parseCustomServers(preferences[Keys.CUSTOM_SERVERS] ?: "[]")
            if (list.any { it.key() == testPoint.key() }) return@edit
            val array = JSONArray()
            (list + testPoint).forEach { array.put(it.toJson()) }
            preferences[Keys.CUSTOM_SERVERS] = array.toString()
        }
    }

    suspend fun removeCustomServer(key: String) {
        context.dataStore.edit { preferences ->
            val list = parseCustomServers(preferences[Keys.CUSTOM_SERVERS] ?: "[]")
            val array = JSONArray()
            list.filter { it.key() != key }.forEach { array.put(it.toJson()) }
            preferences[Keys.CUSTOM_SERVERS] = array.toString()
        }
    }

    private fun parseCustomServers(json: String): List<TestPoint> = try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            try {
                TestPoint(array.getJSONObject(index))
            } catch (_: Exception) {
                null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun TestPoint.toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("server", server)
        .put("dlURL", dlURL)
        .put("ulURL", ulURL)
        .put("pingURL", pingURL)
        .put("getIpURL", getIpURL)

}
