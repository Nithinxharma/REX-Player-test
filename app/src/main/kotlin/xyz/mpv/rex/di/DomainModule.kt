package xyz.mpv.rex.di

import android.content.Context
import androidx.preference.PreferenceManager
import org.koin.dsl.module
import xyz.mpv.rex.cloudstream.CloudStreamPluginCoordinator
import xyz.mpv.rex.preferences.CSCorePreferences
import xyz.mpv.rex.preferences.SubtitlesPreferences
import xyz.mpv.rex.preferences.preference.AndroidPreferenceStore
import xyz.mpv.rex.preferences.preference.PreferenceStore

val domainModule = module {
  single<PreferenceStore> {
    val sp = PreferenceManager.getDefaultSharedPreferences(get<Context>())
    AndroidPreferenceStore(sp)
  }

  single { CSCorePreferences(get()) }
  single { SubtitlesPreferences(get()) }
  single { CloudStreamPluginCoordinator(get()) }
}
