package com.lagradost.cloudstream3.plugins

import android.content.Context
import com.lagradost.cloudstream3.MainAPI
import kotlinx.serialization.Serializable

open class BasePlugin {
    var filename: String? = null
    open fun load(context: Context) {}
    open fun load() {}
    open fun beforeUnload() {}

    @Serializable
    data class Manifest(
        val name: String? = null,
        val pluginClassName: String? = null,
        val version: Int? = 1,
        val description: String? = null,
        val authors: List<String> = emptyList(),
        val url: String? = null
    )
}
