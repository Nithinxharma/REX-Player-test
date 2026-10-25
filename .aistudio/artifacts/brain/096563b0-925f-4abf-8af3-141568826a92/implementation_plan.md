# PrismalAGSL Liquid Glass Material You 3 Integration

Integrate the physics-derived PrismalAGSL liquid glass rendering pipeline across the entire application—from root navigation and bottom bars up to modal sheets, dialogs, media cards, and player controls—seamlessly blending realistic optical glassmorphism with Android Material You 3 dynamic color theming.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed in Phase 1:
> - **Full-App Scope**: PrismalAGSL liquid glass is applied across the root navigation bar, floating action/bottom bars, media library cards, modal sheets, top bars, and in-player playback controls.
> - **Compatibility Fallback**: Android 13+ (API 33+) executes the native AGSL `RuntimeShader` pipeline (Snell refraction, Schlick Fresnel, dual Blinn-Phong specular, spherical meniscus height field). Devices running Android 8.0–12 (API 26–32) automatically fall back to the multi-pass 2D canvas shadow and highlight engine without regressions.
> - **In-Tree Shader Pipeline**: Implemented directly in Kotlin and AGSL without third-party binary artifacts, enabling direct binding to Material 3 dynamic color tokens (`primary`, `surfaceContainer`, `outlineVariant`).

## 1. Overview & Core Concept

- **What It Does**: Upgrades the entire visual shell of the media player and browser from flat/opaque surfaces and standard elevation to a physically simulated "liquid glass" material. Components exhibit real-time optical refraction of underlying content, edge thickness gradients, spherical meniscus bevel highlights, dynamic rim lighting, and subtle chromatic glass reflections while retaining Material You dynamic color harmonizing.
- **Target Audience / Persona**: Media enthusiasts and Android power users who desire an ultra-modern, fluid, tactile OS aesthetic with deep visual hierarchy and seamless responsiveness.
- **Key Value**: Delivers desktop/flagship-class glassmorphism natively on Android using GPU fragment shaders without frame drops, battery drain, or degradation of accessibility and touch targets.

## 2. User Experience & Visual Design

### Key User Flows

1. **Root Navigation & Browsing**:
   - The root bottom `NavigationBar` and `BrowserBottomBar` float above folder and video grids with a curved liquid glass lens. As content scrolls beneath, video thumbnails and typography visibly refract through the lens edges with soft optical distortion and dynamic rim reflections.
2. **Media Cards & Explorer Items**:
   - Video and folder cards utilize subtle glass pill badges, frosted status chips, and refined glass surface cards that reflect ambient colors while maintaining high text contrast and clear touch boundaries.
3. **Modal Sheets & Overlays**:
   - Bottom sheets (Player Sheets, Chapters, Subtitles, Video Quality, Media Info) rise from the bottom with translucent frosted backdrop refraction, lit border highlights at the top edge, and smooth specular gradients.
4. **Player Interface & Controls**:
   - While video playback occurs, floating controls, seekbar containers, chapter pill badges, speed sliders, and gesture volume/brightness indicators render with crystalline refraction over live video frames, keeping the underlying action visible while elevating tactile control clarity.

### Visual Identity & Theme

- **Aesthetic Direction**: *Liquid Specular Minimalist* — combining Android 14/15 Material You dynamic color palettes with physically grounded optical refraction (iOS-style fluid glass meets Material Design 3 Expressive).
- **Color Palette & Atmospheric Tokens**:
  - `Glass Base Tint`: Dynamic `MaterialTheme.colorScheme.surfaceContainer` with calibrated alpha (0.15–0.35) adapting instantly to system wallpaper / dynamic theme switches.
  - `Lit Edge Highlight`: `MaterialTheme.colorScheme.primary` or pure specular white with Schlick Fresnel falloff on directional borders.
  - `Opposite Rim Accent`: M3 `surfaceTint` / `secondary` bounce highlight.
  - `Outer Boundary`: `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)`.
- **Typography & Hierarchy**:
  - Retains existing Google Sans Flex hierarchy with `FontWeight.Medium` and `FontWeight.SemiBold` labels to ensure crisp legibility over refractive glass backgrounds.
- **Component Styling & Layout**:
  - Consistent corner radiuses matching M3 Expressive tokens (`pillShape` 999.dp for bottom pills and action chips, `24.dp` for floating containers, `16.dp` for cards and sheets).
  - Minimum touch target sizing strictly preserved at 48.dp.

### Interactive Feedback & Motion

- **Touch & Press Reaction**: Pressing glass components triggers an interactive refractive surge (subtle highlight shift and scale compression) accompanied by standard Android haptic feedback.
- **Scroll & Movement**: Backdrop sampling dynamically shifts refraction vectors as items glide beneath navigation surfaces.

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native In-Tree AGSL Pipeline vs Remote Maven Dependency**
  - *Chosen Approach*: Embed the complete AGSL shader code, shader uniforms bridge, and Compose modifiers directly within the project's domain/ui graphics layer.
  - *Why*: Eliminates build-time external network dependencies, avoids AGP/KMP binary compatibility issues, and provides direct compile-time access to Compose `ColorScheme` colors and layout constraints.
  - *Alternatives Considered*: Remote JitPack artifact (rejected due to instability, lack of offline reliability, and rigid uniform configurations).

- **Decision 2: Dual-Engine Architecture (AGSL + Canvas Fallback)**
  - *Chosen Approach*: A unified `Modifier.liquidGlass(...)` entry point that inspects `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`. If true, it activates the GPU `RuntimeShader` with `RenderEffect`. If false, it seamlessly redirects parameters to the existing multi-layer canvas inner/outer shadow engine.
  - *Why*: Preserves compatibility with `minSdk = 26` (Android 8.0) while delivering hardware-accelerated refractive glass on Android 13+.
  - *Alternatives Considered*: Enforcing AGSL-only (would break compatibility for users on older Android versions) or using CPU blur (prohibitively slow).

- **Decision 3: Layered Backdrop Capture & GPU Isolation**
  - *Chosen Approach*: Use Compose `graphicsLayer` with `RenderEffect` for self-contained components and parent container backdrop sampling where supported, avoiding full-screen frame buffer stalls.
  - *Why*: Maintains a rock-solid 60/120fps refresh rate on mobile GPUs, even during 4K video playback.
  - *Alternatives Considered*: Full-window bitmap capture every frame (high memory overhead and severe stutter).

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                             Jetpack Compose UI                            │
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌───────────────┐ │
│  │ Navigation / Bar Root │  │ Cards & Explorer Grid │  │ Player Overlays│ │
│  └───────────┬───────────┘  └───────────┬───────────┘  └───────┬───────┘ │
│              │                          │                      │         │
│              ▼                          ▼                      ▼         │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                    Modifier.liquidGlass(...)                        │ │
│  │   • Corner radius & shape bounds     • Material 3 Color Tokens      │ │
│  │   • Refraction index & dispersion    • Specular & Fresnel controls  │ │
│  └──────────────────────────────────┬──────────────────────────────────┘ │
└─────────────────────────────────────┼────────────────────────────────────┘
                                      │
              ┌───────────────────────┴───────────────────────┐
              ▼                                               ▼
   [Android 13+ / API 33+]                         [Android 8 - 12 / API 26-32]
┌───────────────────────────────────┐               ┌──────────────────────────────────┐
│      PrismalAGSL GPU Pipeline     │               │   Multi-Pass Canvas Fallback     │
│  • AGSL RuntimeShader String      │               │   • Directional inner highlight  │
│  • Snell's Law double refraction  │               │   • Clipped outer drop shadow    │
│  • Schlick Fresnel reflectance    │               │   • Soft inner depth shadow      │
│  • Dual Blinn-Phong specular      │               │   • M3 translucent tint stroke   │
│  • Spherical meniscus edge height │               │   • Zero shader crash risk       │
│  • RenderEffect / graphicsLayer   │               │                                  │
└───────────────────────────────────┘               └──────────────────────────────────┘
```

### Data Model & Parameter Specs

```
LiquidGlassSpec(
  shape: Shape,
  refractionStrength: Float,   // e.g. 0.0f - 1.0f (Snell ray deflection)
  ior: Float,                  // Index of refraction (~1.45 - 1.52)
  fresnelPower: Float,         // Schlick exponent (default 5.0)
  specularLightPos: Offset,    // Normalized directional light vector
  specularColor: Color,        // Dynamic M3 highlight color
  tintColor: Color,            // Dynamic M3 surface container color
  borderWidth: Dp,             // Rim edge width
  borderColor: Color,          // Outer stroke tint
  blurRadius: Dp               // Backdrop blur intensity
)
```

### Interactive Component & State Mapping

1. **Navigation Root (`MainScreen.kt`)**:
   - Replaces opaque `NavigationBar` background with `Modifier.liquidGlass(...)` shaped to pill / top-rounded bounds.
   - Preserves tab state transitions (`selectedTab`), haptic pulses, and auto-hide behaviors for the Shorts tab.
2. **Floating Bottom Action Bar (`FloatingBottomBar.kt`)**:
   - Upgrades `Surface` styling to liquid glass pill shape, allowing multi-select buttons to float over active lists with physical edge refraction.
3. **Player HUD & Gesture Overlays (`PlayerControls.kt`, `Seekbar.kt`, `VerticalSliders.kt`)**:
   - Swaps existing `glassSurface` calls to the enhanced `liquidGlass` pipeline. Controls gain realistic optical depth above high-resolution video streams.
4. **Media Explorer Cards (`VideoCard.kt`, `FolderCard.kt`)**:
   - Applies liquid glass accents to duration badges, view-count chips, and card surfaces for consistent polish.
5. **Modal Sheets (`PlayerSheets.kt`, `MediaInfoSheet.kt`)**:
   - Renders sheet surfaces with liquid glass rim highlights, ensuring high readability over complex background content.

### Verification & Performance Plan

- **Compilation Verification**: Validated via `compile_applet`.
- **API Level Graceful Branching**: Validated logic ensuring Android 13+ leverages AGSL while Android 8.0–12 devices execute canvas drawing without runtime crashes.
- **Frame Rate Stability**: Continuous frame budget under 16ms during active 60fps video playback with overlay controls displayed.
