package xyz.mpv.rex.cloudstream

import android.content.Context
import android.content.ContextWrapper
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.utils.Coroutines.atomicListOf
import kotlinx.coroutines.runBlocking
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
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TestMockProvider(
  override var name: String = "MockProvider",
  override var mainUrl: String = "https://mock.cloudstream.test",
  override var supportedTypes: Set<TvType> = setOf(TvType.Movie),
) : MainAPI()

class TestMockPlugin : BasePlugin() {
  override fun load() {
    registerMainAPI(TestMockProvider())
  }
}

class TestFailingPlugin : BasePlugin() {
  override fun load() {
    throw RuntimeException("Simulated plugin crash during load()")
  }
}

class CloudStreamPluginCoordinatorTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var pluginsDir: File

  @Before
  fun setUp() {
    context = ContextWrapper(null)
    pluginsDir = tempFolder.newFolder("Extensions")
    // Ensure APIHolder is clean before each test
    APIHolder.allProviders.clear()
    APIHolder.apis.withLock {
      APIHolder.apis = atomicListOf()
    }
  }

  @After
  fun tearDown() {
    APIHolder.allProviders.clear()
    APIHolder.apis.withLock {
      APIHolder.apis = atomicListOf()
    }
  }

  private fun createPluginZip(
    file: File,
    manifestJson: String? = null,
    includeManifest: Boolean = true,
    manifestEntryName: String = CloudStreamPluginCoordinator.MANIFEST_ENTRY_NAME,
  ) {
    ZipOutputStream(FileOutputStream(file)).use { zos ->
      if (includeManifest) {
        val json = manifestJson ?: """
          {
            "name": "TestMockPlugin",
            "pluginClassName": "${TestMockPlugin::class.java.name}",
            "requiresResources": false,
            "version": 1
          }
        """.trimIndent()
        zos.putNextEntry(ZipEntry(manifestEntryName))
        zos.write(json.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
      }
    }
  }

  @Test
  fun testEmptyPluginDirectoryInitialization() = runBlocking {
    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
    )

    // Registry access before initialization
    assertTrue("Registry should initially be empty", coordinator.registeredProviders.isEmpty())
    assertFalse("Coordinator should not be initialized initially", coordinator.isInitialized)

    val summary = coordinator.initialize()

    assertTrue("Coordinator should be marked as initialized", coordinator.isInitialized)
    assertEquals(0, summary.discoveredFiles)
    assertEquals(0, summary.loadedCount)
    assertEquals(0, summary.failedCount)
    assertEquals(0, summary.totalRegisteredProviders)
    assertTrue(summary.failures.isEmpty())
    assertTrue(coordinator.loadedPlugins.isEmpty())
    assertTrue(coordinator.registeredProviders.isEmpty())
  }

  @Test
  fun testRepeatedInitializationIsIdempotent() = runBlocking {
    val pluginFile = File(pluginsDir, "mock.cs3")
    createPluginZip(pluginFile)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    val firstSummary = coordinator.initialize()
    if (firstSummary.failures.isNotEmpty()) {
      val (file, err) = firstSummary.failures.first()
      throw AssertionError("Load failed for ${file.name}: ${err.message}", err)
    }
    assertEquals(1, firstSummary.discoveredFiles)
    assertEquals(1, firstSummary.loadedCount)
    assertEquals(1, firstSummary.totalRegisteredProviders)

    // Second call should return immediately without re-loading or duplicating providers
    val secondSummary = coordinator.initialize(forceReload = false)
    assertEquals(1, secondSummary.loadedCount)
    assertEquals(1, coordinator.registeredProviders.size)
    assertEquals(1, coordinator.loadedPlugins.size)
  }

  @Test
  fun testMissingOrInvalidPluginArchiveHandling() = runBlocking {
    // 1. File that does not exist or is empty
    val emptyFile = File(pluginsDir, "empty.cs3")
    emptyFile.createNewFile()

    // 2. Corrupt zip archive (random text)
    val corruptFile = File(pluginsDir, "corrupt.cs3")
    corruptFile.writeText("THIS IS NOT A VALID ZIP ARCHIVE")

    // 3. Zip archive without manifest.json
    val noManifestFile = File(pluginsDir, "no_manifest.cs3")
    createPluginZip(noManifestFile, includeManifest = false)

    // 4. Zip archive with invalid/blank pluginClassName
    val invalidManifestFile = File(pluginsDir, "invalid_manifest.cs3")
    createPluginZip(
      invalidManifestFile,
      manifestJson = """{"name": "NoClass", "pluginClassName": "", "version": 1}""",
    )

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
    )

    val summary = coordinator.initialize()

    assertEquals("Should discover all 4 candidate files", 4, summary.discoveredFiles)
    assertEquals("None should have loaded successfully", 0, summary.loadedCount)
    assertEquals("All 4 should be recorded as failures", 4, summary.failedCount)
    assertEquals(4, summary.failures.size)
    assertEquals("No providers should be registered", 0, coordinator.registeredProviders.size)
    assertTrue("Coordinator should still finish initialization without crashing", coordinator.isInitialized)
  }

  @Test
  fun testRegistryAccessBeforeAndAfterInitialization() = runBlocking {
    val pluginFile = File(pluginsDir, "valid.cs3")
    createPluginZip(pluginFile)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    // Verify registry state before init
    assertNull(coordinator.getProviderByName("MockProvider"))
    assertNull(coordinator.getProviderByUrl("https://mock.cloudstream.test"))
    assertEquals(0, coordinator.registeredProviders.size)

    // Initialize
    val summary = coordinator.initialize()
    assertEquals(1, summary.loadedCount)
    assertEquals(1, summary.totalRegisteredProviders)

    // Verify registry state after init
    assertEquals(1, coordinator.registeredProviders.size)
    val provider = coordinator.getProviderByName("MockProvider")
    assertNotNull("Provider should be queryable by name", provider)
    assertEquals("MockProvider", provider?.name)

    val providerByUrl = coordinator.getProviderByUrl("https://mock.cloudstream.test")
    assertNotNull("Provider should be queryable by URL", providerByUrl)
    assertEquals("https://mock.cloudstream.test", providerByUrl?.mainUrl)

    // Verify APIHolder consistency
    val apiHolderProvider = APIHolder.getApiFromNameNull("MockProvider")
    assertNotNull("Provider must be registered directly in APIHolder", apiHolderProvider)
  }

  @Test
  fun testPluginUnloadAndReloadLifecycle() = runBlocking {
    val pluginFile = File(pluginsDir, "test_unload.cs3")
    createPluginZip(pluginFile)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    coordinator.initialize()
    assertEquals(1, coordinator.registeredProviders.size)
    assertEquals(1, coordinator.loadedPlugins.size)

    // Unload the plugin
    val unloaded = coordinator.unloadPlugin(pluginFile)
    assertTrue("Unload should report true", unloaded)
    assertEquals("Loaded plugins list should be empty after unload", 0, coordinator.loadedPlugins.size)
    assertEquals("APIHolder should be emptied of the unloaded provider", 0, coordinator.registeredProviders.size)
    assertNull(coordinator.getProviderByName("MockProvider"))

    // Reload
    val reloadSummary = coordinator.reload()
    assertEquals(1, reloadSummary.loadedCount)
    assertEquals(1, coordinator.registeredProviders.size)
    assertNotNull(coordinator.getProviderByName("MockProvider"))
  }

  @Test
  fun testCapitalManifestEntryDiscovery() = runBlocking {
    val pluginFile = File(pluginsDir, "capital_manifest.cs3")
    createPluginZip(pluginFile, manifestEntryName = "Manifest.json")

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    val summary = coordinator.initialize()
    assertEquals("Should discover and successfully load archive with Manifest.json", 1, summary.loadedCount)
    assertEquals(0, summary.failedCount)
    assertEquals(1, coordinator.registeredProviders.size)
    assertNotNull(coordinator.getProviderByName("MockProvider"))
  }

  @Test
  fun testRealWorldManifestWithExtraMetadataFields() = runBlocking {
    val pluginFile = File(pluginsDir, "rich_manifest.cs3")
    val richManifestJson = """
      {
        "name": "SuperStream",
        "pluginClassName": "${TestMockPlugin::class.java.name}",
        "version": 2,
        "requiresResources": false,
        "author": "Lagradost",
        "description": "Stream movie and TV show links",
        "repositoryUrl": "https://github.com/recloudstream/cloudstream-extensions",
        "status": 1
      }
    """.trimIndent()
    createPluginZip(pluginFile, manifestJson = richManifestJson)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    val summary = coordinator.initialize()
    assertEquals(1, summary.loadedCount)
    assertEquals(0, summary.failedCount)
    val loaded = coordinator.loadedPlugins.first()
    assertEquals("SuperStream", loaded.manifest.name)
    assertEquals(2, loaded.manifest.version)
    assertEquals(1, coordinator.registeredProviders.size)
  }

  @Test
  fun testMissingEntryClassRecordsFailureGracefully() = runBlocking {
    val pluginFile = File(pluginsDir, "missing_class.cs3")
    val missingClassManifest = """
      {
        "name": "MissingClassPlugin",
        "pluginClassName": "com.lagradost.nonexistent.NoSuchPluginClass",
        "version": 1
      }
    """.trimIndent()
    createPluginZip(pluginFile, manifestJson = missingClassManifest)

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = AndroidPluginClassLoaderFactory,
    )

    val summary = coordinator.initialize()
    assertEquals(0, summary.loadedCount)
    assertEquals(1, summary.failedCount)
    assertEquals(1, summary.failures.size)
    val (failedFile, error) = summary.failures.first()
    assertEquals(pluginFile.name, failedFile.name)
    assertTrue("Error should mention missing class or instantiation failure", error.message?.contains("NoSuchPluginClass") == true || error.cause is ClassNotFoundException)
    assertTrue(coordinator.isInitialized)
  }

  @Test
  fun testNonBasePluginClassRecordsFailureGracefully() = runBlocking {
    val pluginFile = File(pluginsDir, "invalid_inheritance.cs3")
    val invalidInheritanceManifest = """
      {
        "name": "InvalidInheritancePlugin",
        "pluginClassName": "java.lang.String",
        "version": 1
      }
    """.trimIndent()
    createPluginZip(pluginFile, manifestJson = invalidInheritanceManifest)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    val summary = coordinator.initialize()
    assertEquals(0, summary.loadedCount)
    assertEquals(1, summary.failedCount)
    val (failedFile, error) = summary.failures.first()
    assertEquals(pluginFile.name, failedFile.name)
    assertTrue("Error should indicate class does not extend BasePlugin", error.message?.contains("does not extend BasePlugin") == true)
  }

  @Test
  fun testPluginLoadExceptionIsIsolated() = runBlocking {
    val failingPluginFile = File(pluginsDir, "failing.cs3")
    val failingManifest = """
      {
        "name": "FailingPlugin",
        "pluginClassName": "${TestFailingPlugin::class.java.name}",
        "version": 1
      }
    """.trimIndent()
    createPluginZip(failingPluginFile, manifestJson = failingManifest)

    val validPluginFile = File(pluginsDir, "valid.cs3")
    createPluginZip(validPluginFile)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    val summary = coordinator.initialize()
    assertEquals("Should discover 2 files", 2, summary.discoveredFiles)
    assertEquals("1 valid plugin should have loaded successfully", 1, summary.loadedCount)
    assertEquals("1 failing plugin should be recorded as failure", 1, summary.failedCount)
    assertEquals(1, summary.failures.size)
    assertEquals(failingPluginFile.name, summary.failures.first().first.name)
    assertEquals("Valid provider should be registered in APIHolder", 1, coordinator.registeredProviders.size)
    assertNotNull(coordinator.getProviderByName("MockProvider"))
  }

  @Test
  fun testNoDuplicateProviderRegistrationOnReInit() = runBlocking {
    val pluginFile = File(pluginsDir, "singleton.cs3")
    createPluginZip(pluginFile)

    val testClassLoaderFactory = PluginClassLoaderFactory { _, _ ->
      TestMockPlugin::class.java.classLoader!!
    }

    val coordinator = CloudStreamPluginCoordinator(
      context = context,
      pluginsDirectory = pluginsDir,
      classLoaderFactory = testClassLoaderFactory,
    )

    coordinator.initialize()
    assertEquals(1, coordinator.registeredProviders.size)

    // Second initialization call without forceReload
    coordinator.initialize(forceReload = false)
    assertEquals(1, coordinator.registeredProviders.size)
    assertEquals(1, APIHolder.allProviders.size)

    // Third call with forceReload (unload + reload)
    val reloadSummary = coordinator.initialize(forceReload = true)
    assertEquals(1, reloadSummary.loadedCount)
    assertEquals(1, coordinator.registeredProviders.size)
    assertEquals(1, APIHolder.allProviders.size)
  }
}
