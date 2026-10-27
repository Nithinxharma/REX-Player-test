package xyz.mpv.rex.cloudstream

import android.content.Context
import android.content.ContextWrapper
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.utils.Coroutines.atomicListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.preferences.preference.Preference
import xyz.mpv.rex.preferences.preference.PreferenceStore
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class REXRepositoryManagerTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var pluginsDir: File
  private lateinit var okHttpClient: OkHttpClient
  private lateinit var pluginCoordinator: CloudStreamPluginCoordinator
  private lateinit var preferences: CSCorePreferences
  private lateinit var repositoryManager: REXRepositoryManager

  class TestPreference<T>(private var value: T) : Preference<T> {
    private val flow = MutableStateFlow(value)
    override fun key(): String = "test_key"
    override fun get(): T = value
    override fun set(value: T) {
      this.value = value
      flow.value = value
    }
    override fun isSet(): Boolean = true
    override fun delete() {}
    override fun defaultValue(): T = value
    override fun changes(): Flow<T> = flow
    override fun stateIn(scope: kotlinx.coroutines.CoroutineScope): kotlinx.coroutines.flow.StateFlow<T> = flow
  }

  class TestPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun getString(key: String, defaultValue: String): Preference<String> {
      return TestPreference(values[key] as? String ?: defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = TestPreference(defaultValue)
    override fun getInt(key: String, defaultValue: Int): Preference<Int> = TestPreference(defaultValue)
    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = TestPreference(defaultValue)
    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = TestPreference(defaultValue)

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
      return TestPreference(values[key] as? Set<String> ?: defaultValue)
    }

    override fun <T> getObject(key: String, defaultValue: T, serializer: (T) -> String, deserializer: (String) -> T): Preference<T> {
      return TestPreference(defaultValue)
    }

    override fun getAll(): Map<String, *> = values
  }

  @Before
  fun setUp() {
    context = ContextWrapper(null)
    pluginsDir = tempFolder.newFolder("Extensions")

    APIHolder.allProviders.clear()
    APIHolder.apis.withLock {
      APIHolder.apis = atomicListOf()
    }

    okHttpClient = OkHttpClient()
    pluginCoordinator = CloudStreamPluginCoordinator(context, pluginsDir) { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    preferences = CSCorePreferences(TestPreferenceStore())
    repositoryManager = REXRepositoryManager(context, okHttpClient, pluginCoordinator, preferences)
  }

  @After
  fun tearDown() {
    APIHolder.allProviders.clear()
    APIHolder.apis.withLock {
      APIHolder.apis = atomicListOf()
    }
  }

  private fun createPluginZipFile(file: File) {
    ZipOutputStream(java.io.FileOutputStream(file)).use { zos ->
      val json = """
        {
          "name": "TestMockPlugin",
          "pluginClassName": "${TestMockPlugin::class.java.name}",
          "requiresResources": false,
          "version": 1
        }
      """.trimIndent()
      zos.putNextEntry(ZipEntry(CloudStreamPluginCoordinator.MANIFEST_ENTRY_NAME))
      zos.write(json.toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()
    }
  }

  @Test
  fun testGetRepositoriesReturnsPrebuiltAndCustom() {
    val initialRepos = repositoryManager.getRepositories()
    assertTrue(initialRepos.isNotEmpty())
    assertEquals("MegaRepo", initialRepos.first().name)

    preferences.addCustomRepository("CustomRepo", "https://custom.test/repo.json")
    val updatedRepos = repositoryManager.getRepositories()
    assertEquals(initialRepos.size + 1, updatedRepos.size)
    assertTrue(updatedRepos.any { it.url == "https://custom.test/repo.json" })
  }

  @Test
  fun testLocalPluginDiscoveryAndUninstall() = runBlocking {
    val pluginFile = File(pluginsDir, "InstalledPlugin.cs3")
    createPluginZipFile(pluginFile)

    val localUuid = Uuid.random()
    val localPlugins = repositoryManager.getLocalPlugins(localUuid)

    assertEquals(1, localPlugins.size)
    val localPlugin = localPlugins.first()
    assertEquals("TestMockPlugin", localPlugin.info.name)
    assertEquals(pluginFile.absolutePath, localPlugin.downloadLocation)

    // Load plugin
    pluginCoordinator.loadPluginArchive(pluginFile)
    assertNotNull("Provider should be registered in APIHolder", APIHolder.getApiFromNameNull("MockProvider"))

    // Uninstall via RepositoryManager
    repositoryManager.deletePlugin(pluginFile.absolutePath)
    assertFalse("Plugin file should be deleted", pluginFile.exists())
    assertNull("Provider should be removed after uninstall", APIHolder.getApiFromNameNull("MockProvider"))
  }
}
