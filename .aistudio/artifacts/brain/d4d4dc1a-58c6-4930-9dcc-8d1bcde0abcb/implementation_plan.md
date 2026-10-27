# Master Implementation Plan: 100% CloudStream Integration in REX Player

Comprehensive technical blueprint for migrating complete non-player CloudStream functionality into REX Player's native Compose architecture, preserving REX's MPV playback engine, dark Material 3 design system, and unified media management.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed by the user:

- **Confirmed Decision 1 (Home Feeds)**: Dynamic section rows on Home are driven directly by active extensions (`MainAPI.getMainPage()`), displaying trending and catalog feeds alongside local media history.
- **Confirmed Decision 2 (Account Integrations)**: Third-party tracker and subtitle accounts (OpenSubtitles, AniList, MyAnimeList, Kitsu, Simkl) are configured via sub-menus under Subtitle and Tracker Preferences.
- **Confirmed Decision 3 (Downloads)**: CloudStream extension downloads are integrated directly into REX Player's single unified download manager and queue.
- **Confirmed Decision 4 (Player Core)**: CloudStream internal ExoPlayer/VLC player views are strictly excluded. All streams route directly to REX's high-performance native MPV player engine (`PlayerActivity`).
- **Confirmed Decision 5 (Architecture)**: `cloudstream/` source directory is treated as read-only specification. All new code is implemented in REX's native `xyz.mpv.rex` package structure.

---

## 1. Overview & Core Concept

### What It Does
This initiative brings 100% of CloudStream's non-player capabilities (provider search, home catalog discovery, details page extraction, repository management, extension updates, tracker synchronization, subtitle fetching, and background download queueing) into REX Player's Jetpack Compose UI while utilizing REX's MPV video engine for stream playback.

### Target Audience & Persona
Power users who want a single, unified Android media player that seamlessly combines local video playing, SMB/SFTP network streaming, Jellyfin media server sync, and CloudStream provider extensions without switching between separate applications.

### Key Value
- **Zero Mock UI**: Every screen, card, and setting is backed by actual `MainAPI` providers and CloudStream core logic.
- **Unified Engine**: Streams extracted from CloudStream providers automatically leverage REX Player's advanced MPV shaders (Anime4K, HDR-Toys), hardware decoding, and gesture controls.
- **Full Extension Management**: Seamless `.cs3` plugin installation, updating, and repository JSON management.

---

## 2. Visual Design & User Experience

### Aesthetic Direction
- **Theme**: REX Player OLED Dark / Material 3 Expressive.
- **Layouts**: M3 Grouped Cards (`GroupedListColumn`), asymmetric media rows with subtle elevation, responsive grid layouts for TV and mobile form factors.
- **Components**: Standardized Material 3 chips, sliders, text fields, and progress indicators.

### User Workflows

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         REX Player Compose UI                            │
├──────────────────────────┬───────────────────────────┬───────────────────┤
│       Home Screen        │       Search Screen       │ Settings & CSCore │
│ (Local + Dynamic Feeds)  │ (Local + Provider Search) │ (Repos & Trackers)│
└─────────────┬────────────┴─────────────┬─────────────┴─────────┬─────────┘
              │                          │                       │
              ▼                          ▼                       ▼
┌──────────────────────────┐┌──────────────────────────┐┌────────────────┐
│  CloudStream MainPage    ││ Aggregated Provider Query││ Repository &   │
│ (Active Extension Feeds) ││ (All Active MainAPIs)    ││ Plugin Manager │
└─────────────┬────────────┘└─────────────┬────────────┘└───────┬────────┘
              │                           │                     │
              └────────────────────┬──────┘                     │
                                   ▼                            ▼
                     ┌───────────────────────────┐    ┌──────────────────┐
                     │ CloudStream Detail Screen │    │ Background Sync  │
                     │ (Seasons, Episodes, Subs) │    │ & Auto Updates   │
                     └─────────────┬─────────────┘    └──────────────────┘
                                   │
                                   ▼
                     ┌───────────────────────────┐
                     │    Stream Link Extractor  │
                     └─────────────┬─────────────┘
                                   │
                                   ▼
                     ┌───────────────────────────┐
                     │    REX MPV Player Core    │
                     │  (Hardware Accel, Shaders)│
                     └───────────────────────────┘
```

---

## 3. System Architecture & Technical Strategy

### Component Architecture & Module Mapping

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        xyz.mpv.rex (REX Application)                       │
├────────────────────────────────────────────────────────────────────────────┤
│ [UI Presentation Layer]                                                    │
│  ├── PreferencesScreen & CSCorePreferencesScreen                            │
│  ├── SearchScreen (Local Files + CloudStream APIHolder Search)             │
│  └── Details / Episode Selection Sheets                                    │
├────────────────────────────────────────────────────────────────────────────┤
│ [Domain & Business Logic Layer]                                            │
│  ├── CloudStreamPluginCoordinator (Extension Archive & ClassLoader Engine) │
│  ├── CSCorePreferences (Preferences & Account Key Bridge)                 │
│  └── NetworkRepository / MediaFileRepository / DownloadQueueManager        │
├────────────────────────────────────────────────────────────────────────────┤
│ [Read-Only Core Engine]                                                    │
│  ├── cloudstream/library (com.lagradost.cloudstream3.*)                    │
│  └── cloudstream/shared (com.lagradost.cloudstream4.*)                     │
└────────────────────────────────────────────────────────────────────────────┘
```

### Key Technical Strategies

1. **Provider Aggregation & Execution**:
   - `CloudStreamPluginCoordinator` discovers, verifies, and loads `.cs3` files into ART via custom ClassLoaders.
   - Provider instances register into `com.lagradost.cloudstream3.APIHolder`.

2. **Dynamic Home & Search Feeds**:
   - Home screen executes `MainAPI.getMainPage()` for enabled extensions to display dynamic content categories.
   - Global Search queries active providers in parallel and renders responsive result grids.

3. **Stream Extraction & MPV Hand-off**:
   - Extractors (`ExtractorApi`) resolve stream URLs and HTTP headers (`User-Agent`, `Referer`).
   - Resolved streams launch REX's native `PlayerActivity` (`libmpv`), preserving hardware acceleration and custom shaders.

---

## 4. Migration & Execution Phases

### Phase 1: Source & Feature Inventory
- Audit original CloudStream source files under `cloudstream/` and map responsibilities to REX target files.
- Verify read-only status of `cloudstream/library/**` and `cloudstream/shared/**`.

### Phase 2: Core Infrastructure & Preferences Bridge
- Maintain `CSCorePreferences` to bridge shared keys (`REPOSITORIES_KEY`, `auto_update_plugins`, `dns_key`, `display_sub_key`, tracker tokens).
- Verify `CloudStreamPluginCoordinator` scanning, loading, and provider registration.

### Phase 3: Dynamic Home Feeds Integration
- Connect active `MainAPI` home pages to REX Player's Home screen, displaying provider catalogs in dynamic horizontal rows alongside local watch history.

### Phase 4: Provider Search & Content Discovery
- Aggregate provider search results into `SearchScreen`.
- Support language filters, media type filters (Movie, TV Series, Anime, Cartoon, Documentary), and live search.

### Phase 5: Content Detail Page & Extractor Hand-off
- Display media metadata, poster, background art, genres, cast, season/episode selector, and subtitle choices.
- Connect stream extraction directly to REX's MPV player core.

### Phase 6: Repository & Extensions Management UI
- Implement Repository Manager screen in Compose matching the CloudStream screenshot reference.
- Support adding custom repository URLs, removing repositories, viewing installed `.cs3` plugins, updating, and filtering by category.

### Phase 7: Account Sync, Trackers & Diagnostics
- Sub-menu preferences for OpenSubtitles, SubDL, AniList, MyAnimeList, Kitsu, and Simkl account credentials.
- Diagnostics logger and logcat viewer.

### Phase 8: Verification & Testing
- Execute Gradle build and test verification (`compile_applet`).
- Ensure no regressions in existing REX local playback, MPV controls, or Jellyfin features.
