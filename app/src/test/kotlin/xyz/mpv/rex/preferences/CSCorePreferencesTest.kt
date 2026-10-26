package xyz.mpv.rex.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.mpv.rex.preferences.preference.Preference
import xyz.mpv.rex.preferences.preference.PreferenceStore
import xyz.mpv.rex.ui.preferences.SearchablePreferences

class CSCorePreferencesTest {

  private class InMemoryPreference<T>(
    private val key: String,
    private val defaultValue: T,
  ) : Preference<T> {
    private val flow = MutableStateFlow(defaultValue)

    override fun key(): String = key
    override fun get(): T = flow.value
    override fun set(value: T) { flow.value = value }
    override fun isSet(): Boolean = true
    override fun delete() { flow.value = defaultValue }
    override fun defaultValue(): T = defaultValue
    override fun changes(): Flow<T> = flow.asStateFlow()
    override fun stateIn(scope: CoroutineScope) = flow.asStateFlow()
  }

  private class InMemoryPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, Any?>()

    override fun getString(key: String, defaultValue: String): Preference<String> =
      InMemoryPreference(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
      InMemoryPreference(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
      InMemoryPreference(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
      InMemoryPreference(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
      InMemoryPreference(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
      InMemoryPreference(key, defaultValue)

    override fun <T> getObject(
      key: String,
      defaultValue: T,
      serializer: (T) -> String,
      deserializer: (String) -> T,
    ): Preference<T> = InMemoryPreference(key, defaultValue)

    override fun getAll(): Map<String, *> = values
  }

  @Test
  fun cscorePreferences_keysAndDefaultsMatchCloudStreamCore() {
    val store = InMemoryPreferenceStore()
    val preferences = CSCorePreferences(store)

    // Key alignment
    assertEquals("REPOSITORIES_KEY", CSCorePreferences.KEY_REPOSITORIES)
    assertEquals("auto_update_plugins", CSCorePreferences.KEY_AUTO_UPDATE_PLUGINS)
    assertEquals("auto_download_plugins_key2", CSCorePreferences.KEY_AUTO_DOWNLOAD_PLUGINS)
    assertEquals("prefer_media_type_key_2", CSCorePreferences.KEY_PREFER_MEDIA_TYPE)
    assertEquals("provider_lang_key", CSCorePreferences.KEY_PROVIDER_LANG)
    assertEquals("display_sub_key", CSCorePreferences.KEY_DISPLAY_SUB)
    assertEquals("dns_key", CSCorePreferences.KEY_DNS)
    assertEquals("jsdelivr_proxy_key", CSCorePreferences.KEY_JSDELIVR_PROXY)

    // Default values
    assertTrue(preferences.autoUpdatePlugins.get())
    assertEquals(0, preferences.autoDownloadPlugins.get())
    assertEquals(0, preferences.dnsProvider.get())
    assertFalse(preferences.jsdelivrProxy.get())
    assertEquals(3, preferences.parallelDownloads.get())
    assertEquals(3, preferences.concurrentConnections.get())
  }

  @Test
  fun repositories_defaultsToPrebuilt_andHandlesCustomRepos() {
    val store = InMemoryPreferenceStore()
    val preferences = CSCorePreferences(store)

    val repos = preferences.getRepositories()
    assertTrue(repos.size >= 3)
    assertTrue(repos.any { it.name == "MegaRepo" })

    // Add custom repository
    preferences.addCustomRepository("TestRepo", "https://example.com/repo.json")
    val updatedRepos = preferences.getRepositories()
    assertTrue(updatedRepos.any { it.url == "https://example.com/repo.json" })

    // Remove custom repository
    preferences.removeCustomRepository("https://example.com/repo.json")
    val afterRemoval = preferences.getRepositories()
    assertFalse(afterRemoval.any { it.url == "https://example.com/repo.json" })
  }

  @Test
  fun nsfwToggle_mutatesPreferredMediaTypesSafely() {
    val store = InMemoryPreferenceStore()
    val preferences = CSCorePreferences(store)

    assertFalse(preferences.isNsfwAllowed())

    preferences.setNsfwAllowed(true)
    assertTrue(preferences.isNsfwAllowed())

    preferences.setNsfwAllowed(false)
    assertFalse(preferences.isNsfwAllowed())
  }

  @Test
  fun searchablePreferences_indexesCSCore() {
    val cscoreEntry = SearchablePreferences.allPreferences.find {
      it.titleRes == xyz.mpv.rex.R.string.pref_cscore_title
    }

    assertNotNull(cscoreEntry)
    assertEquals(xyz.mpv.rex.R.string.pref_cscore_summary, cscoreEntry?.summaryRes)
    assertEquals("Integrations", cscoreEntry?.category)
    assertTrue(cscoreEntry?.keywords?.contains("cscore") == true)
    assertTrue(cscoreEntry?.keywords?.contains("cloudstream") == true)
  }
}
