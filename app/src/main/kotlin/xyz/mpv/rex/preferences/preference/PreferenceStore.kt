package xyz.mpv.rex.preferences.preference

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PreferenceStore {
  fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean>
  fun getInt(key: String, defaultValue: Int): Preference<Int>
  fun getString(key: String, defaultValue: String): Preference<String>
  fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>>
}

interface Preference<T> {
  fun get(): T
  fun set(value: T)
  fun stateIn(scope: kotlinx.coroutines.CoroutineScope): Flow<T>
}

class AndroidPreference<T>(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val defaultValue: T,
  private val getter: (SharedPreferences, String, T) -> T,
  private val setter: (SharedPreferences.Editor, String, T) -> Unit,
) : Preference<T> {
  private val _flow = MutableStateFlow(get())

  override fun get(): T {
    return getter(sharedPreferences, key, defaultValue)
  }

  override fun set(value: T) {
    sharedPreferences.edit().also { setter(it, key, value) }.apply()
    _flow.value = value
  }

  override fun stateIn(scope: kotlinx.coroutines.CoroutineScope): Flow<T> {
    return _flow.asStateFlow()
  }
}

class AndroidPreferenceStore(
  private val sharedPreferences: SharedPreferences,
) : PreferenceStore {
  override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
    return AndroidPreference(sharedPreferences, key, defaultValue, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean)
  }

  override fun getInt(key: String, defaultValue: Int): Preference<Int> {
    return AndroidPreference(sharedPreferences, key, defaultValue, SharedPreferences::getInt, SharedPreferences.Editor::putInt)
  }

  override fun getString(key: String, defaultValue: String): Preference<String> {
    return AndroidPreference(
      sharedPreferences,
      key,
      defaultValue,
      { sp, k, def -> sp.getString(k, def) ?: def },
      SharedPreferences.Editor::putString,
    )
  }

  override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
    return AndroidPreference(
      sharedPreferences,
      key,
      defaultValue,
      { sp, k, def -> sp.getStringSet(k, def) ?: def },
      SharedPreferences.Editor::putStringSet,
    )
  }
}
