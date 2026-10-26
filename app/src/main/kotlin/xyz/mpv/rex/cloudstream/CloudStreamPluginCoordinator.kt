package xyz.mpv.rex.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

/**
 * Factory abstraction for creating [ClassLoader] instances for plugins.
 * Enables Android Dalvik/ART [dalvik.system.PathClassLoader] in production
 * while supporting custom/JVM ClassLoaders in tests.
 */
fun interface PluginClassLoaderFactory {
  fun createClassLoader(pluginFile: File, parent: ClassLoader): ClassLoader
}

object AndroidPluginClassLoaderFactory : PluginClassLoaderFactory {
  override fun createClassLoader(pluginFile: File, parent: ClassLoader): ClassLoader {
    return runCatching {
      dalvik.system.PathClassLoader(pluginFile.absolutePath, parent)
    }.getOrElse {
      java.net.URLClassLoader(arrayOf(pluginFile.toURI().toURL()), parent)
    }
  }
}

data class LoadedPlugin(
  val file: File,
  val manifest: BasePlugin.Manifest,
  val instance: BasePlugin,
  val registeredProviders: List<MainAPI>,
)

sealed class PluginLoadResult {
  data class Success(val loadedPlugin: LoadedPlugin) : PluginLoadResult()
  data class Failure(val file: File, val error: Throwable) : PluginLoadResult()
}

data class PluginCoordinatorSummary(
  val discoveredFiles: Int,
  val loadedCount: Int,
  val failedCount: Int,
  val totalRegisteredProviders: Int,
  val failures: List<Pair<File, Throwable>> = emptyList(),
)

/**
 * Headless Plugin Storage & Registry Coordinator for REX.
 *
 * Responsibilities:
 * - Scans the standard CloudStream storage path (context.filesDir/Extensions) for ".cs3" plugins.
 * - Safely inspects and extracts "manifest.json" from plugin archives.
 * - Dynamically loads and instantiates compatible [BasePlugin] classes.
 * - Coordinates provider registration with CloudStream's [APIHolder].
 * - Ensures thread-safe, idempotent initialization and failure-isolated execution off the main thread.
 */
class CloudStreamPluginCoordinator(
  private val context: Context,
  val pluginsDirectory: File = File(context.filesDir, EXTENSIONS_FOLDER),
  private val classLoaderFactory: PluginClassLoaderFactory = AndroidPluginClassLoaderFactory,
) {

  companion object {
    const val EXTENSIONS_FOLDER = "Extensions"
    const val PLUGIN_EXTENSION = ".cs3"
    const val MANIFEST_ENTRY_NAME = "manifest.json"
    private const val TAG = "CSPluginCoordinator"
  }

  private val initMutex = Mutex()
  private val _isInitialized = AtomicBoolean(false)
  val isInitialized: Boolean get() = _isInitialized.get()

  private val _loadedPlugins = Collections.synchronizedList(mutableListOf<LoadedPlugin>())
  val loadedPlugins: List<LoadedPlugin>
    get() = synchronized(_loadedPlugins) { _loadedPlugins.toList() }

  /**
   * Snapshot of all registered CloudStream [MainAPI] providers in [APIHolder].
   */
  val registeredProviders: List<MainAPI>
    get() = APIHolder.allProviders.withLock { APIHolder.allProviders.toList() }

  /**
   * Initializes the plugin subsystem:
   * - Ensures the plugin directory exists.
   * - Discovers all installed ".cs3" plugins.
   * - Safely parses manifests and loads plugins without blocking the main thread.
   * - Idempotent: repeated calls without [forceReload] return the current summary immediately.
   */
  suspend fun initialize(forceReload: Boolean = false): PluginCoordinatorSummary = initMutex.withLock {
    if (_isInitialized.get() && !forceReload) {
      return buildSummary(failures = emptyList())
    }

    withContext(Dispatchers.IO) {
      if (!pluginsDirectory.exists()) {
        pluginsDirectory.mkdirs()
      }

      val discoveredFiles = discoverPluginArchives(pluginsDirectory)
      val failures = mutableListOf<Pair<File, Throwable>>()

      for (file in discoveredFiles) {
        when (val result = loadPluginArchive(file)) {
          is PluginLoadResult.Success -> {
            // Success recorded in _loadedPlugins inside loadPluginArchive
          }
          is PluginLoadResult.Failure -> {
            failures.add(file to result.error)
          }
        }
      }

      _isInitialized.set(true)
      buildSummary(failures)
    }
  }

  /**
   * Recursively finds all ".cs3" plugin archives in the target directory.
   */
  fun discoverPluginArchives(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
      return emptyList()
    }

    return directory.walkTopDown()
      .filter { it.isFile && it.name.endsWith(PLUGIN_EXTENSION, ignoreCase = true) }
      .toList()
  }

  /**
   * Loads a single plugin archive file safely:
   * 1. Validates file existence and readability.
   * 2. Reads and parses "manifest.json".
   * 3. Creates a ClassLoader via [classLoaderFactory].
   * 4. Instantiates and loads the [BasePlugin] class.
   * 5. Captures registered [MainAPI] instances and records the plugin.
   */
  suspend fun loadPluginArchive(file: File): PluginLoadResult = withContext(Dispatchers.IO) {
    if (!file.exists() || !file.isFile || file.length() == 0L) {
      return@withContext PluginLoadResult.Failure(
        file,
        IllegalArgumentException("Plugin file is missing or empty: ${file.absolutePath}"),
      )
    }

    // If already loaded from this path, unload it first
    unloadPlugin(file)

    val manifest = runCatching { readManifest(file) }.getOrElse { throwable ->
      return@withContext PluginLoadResult.Failure(
        file,
        IllegalStateException("Failed to read manifest from ${file.name}: ${throwable.message}", throwable),
      )
    } ?: return@withContext PluginLoadResult.Failure(
      file,
      IllegalStateException("Archive ${file.name} does not contain a valid $MANIFEST_ENTRY_NAME"),
    )

    val className = manifest.pluginClassName
    if (className.isNullOrBlank()) {
      return@withContext PluginLoadResult.Failure(
        file,
        IllegalStateException("Plugin manifest in ${file.name} is missing 'pluginClassName'"),
      )
    }

    val providersBefore = APIHolder.allProviders.withLock { APIHolder.allProviders.toSet() }

    val pluginInstance = runCatching {
      val parentLoader = runCatching { context.classLoader }.getOrNull()
        ?: javaClass.classLoader
        ?: ClassLoader.getSystemClassLoader()
      val classLoader = classLoaderFactory.createClassLoader(file, parentLoader)
      val clazz = Class.forName(className, true, classLoader)
      if (!BasePlugin::class.java.isAssignableFrom(clazz)) {
        throw IllegalArgumentException("Class $className does not extend BasePlugin")
      }
      val instance = clazz.getDeclaredConstructor().newInstance() as BasePlugin
      instance.filename = file.absolutePath

      // Invoke load lifecycle: attempt load(context) if present, else fallback to load()
      try {
        val contextMethod = clazz.getMethod("load", Context::class.java)
        contextMethod.invoke(instance, context)
      } catch (_: NoSuchMethodException) {
        instance.load()
      }

      instance
    }.getOrElse { throwable ->
      return@withContext PluginLoadResult.Failure(
        file,
        IllegalStateException("Failed to instantiate or load plugin $className: ${throwable.message}", throwable),
      )
    }

    val providersAfter = APIHolder.allProviders.withLock { APIHolder.allProviders.toList() }
    val newProviders = providersAfter.filterNot { providersBefore.contains(it) }

    val loaded = LoadedPlugin(
      file = file,
      manifest = manifest,
      instance = pluginInstance,
      registeredProviders = newProviders,
    )

    synchronized(_loadedPlugins) {
      _loadedPlugins.add(loaded)
    }

    PluginLoadResult.Success(loaded)
  }

  /**
   * Safely reads and parses "manifest.json" from a ZIP archive.
   */
  fun readManifest(file: File): BasePlugin.Manifest? {
    ZipFile(file).use { zip ->
      val entry = zip.getEntry(MANIFEST_ENTRY_NAME)
        ?: zip.getEntry("Manifest.json")
        ?: zip.entries().asSequence().firstOrNull { it.name.equals(MANIFEST_ENTRY_NAME, ignoreCase = true) }
        ?: return null
      val jsonContent = zip.getInputStream(entry).bufferedReader().use { it.readText() }
      return AppUtils.tryParseJson<BasePlugin.Manifest>(jsonContent)
    }
  }

  /**
   * Unloads a plugin associated with [file], invoking [BasePlugin.beforeUnload]
   * and unregistering its providers from [APIHolder].
   */
  suspend fun unloadPlugin(file: File): Boolean = withContext(Dispatchers.IO) {
    val target = synchronized(_loadedPlugins) {
      _loadedPlugins.firstOrNull { it.file.canonicalPath == file.canonicalPath }
    } ?: return@withContext false

    runCatching {
      target.instance.beforeUnload()
    }

    APIHolder.allProviders.withLock {
      for (provider in target.registeredProviders) {
        APIHolder.allProviders.remove(provider)
        APIHolder.removePluginMapping(provider)
      }
    }

    synchronized(_loadedPlugins) {
      _loadedPlugins.remove(target)
    }

    true
  }

  /**
   * Unloads all currently loaded plugins and re-scans the plugin directory.
   */
  suspend fun reload(): PluginCoordinatorSummary = initMutex.withLock {
    withContext(Dispatchers.IO) {
      val loadedCopy = synchronized(_loadedPlugins) { _loadedPlugins.toList() }
      for (loaded in loadedCopy) {
        unloadPlugin(loaded.file)
      }
    }
    initialize(forceReload = true)
  }

  /**
   * Query a registered provider by name using [APIHolder].
   */
  fun getProviderByName(name: String): MainAPI? {
    return APIHolder.getApiFromNameNull(name)
  }

  /**
   * Query a registered provider by URL using [APIHolder].
   */
  fun getProviderByUrl(url: String): MainAPI? {
    return APIHolder.getApiFromUrlNull(url)
  }

  private fun buildSummary(failures: List<Pair<File, Throwable>>): PluginCoordinatorSummary {
    val loaded = loadedPlugins
    val totalProviders = registeredProviders.size
    return PluginCoordinatorSummary(
      discoveredFiles = loaded.size + failures.size,
      loadedCount = loaded.size,
      failedCount = failures.size,
      totalRegisteredProviders = totalProviders,
      failures = failures,
    )
  }
}
