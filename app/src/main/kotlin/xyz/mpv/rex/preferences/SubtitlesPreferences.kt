package xyz.mpv.rex.preferences

import xyz.mpv.rex.preferences.preference.PreferenceStore

class SubtitlesPreferences(
  preferenceStore: PreferenceStore,
) {
  val subdlApiKey = preferenceStore.getString("subdl_api_key", "")
}
