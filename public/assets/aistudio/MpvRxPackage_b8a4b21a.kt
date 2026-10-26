package com.lagradost.cloudstream3.actions.temp

/**
 * REX Player integration.
 *
 * Reuses the same intent extras and playback handling as MpvExPackage via
 * MpvPackage, while targeting REX Player's own application ID and Activity.
 *
 * REX Player repository:
 * https://github.com/mpvRex/REX-Player
 */
class MpvRxPackage : MpvPackage(
    appName = "REX Player",
    packageName = "xyz.mpv.rex",
    intentClass = "xyz.mpv.rex.ui.player.PlayerActivity"
)
