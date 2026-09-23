# Tech decisions

Versions checked on Maven Central / Google Maven on 2026-09-23.

## Platform

| Area | Choice | Alternatives | Why |
|---|---|---|---|
| Language | Kotlin 2.4.20 | Java | Kotlin-first platform. Coroutines, Compose need it. |
| UI | Jetpack Compose (BOM 2026.09.00) | XML Views; Flutter; Compose Multiplatform | Views: legacy, more code. Flutter: no native Material 3 Expressive, harder system integration (overlay, services). CMP: iOS cannot do unlock detection, so no gain. |
| minSdk | 31 (Android 12) | 26, 29 | 31 gives Material You dynamic color and the splash API natively. Loses devices older than 2021. |
| targetSdk / compileSdk | 37 | 36 | 37 is installed and newest. Play needs a recent target anyway. |
| Build | AGP 9.4.1 + Gradle 9.7.1, version catalog | AGP 8.x | AGP 9 has built-in Kotlin (no `kotlin-android` plugin). Newest stable. |
| Annotation processing | KSP 2.3.12 | kapt | kapt is in maintenance and slow. Room 3 is KSP-only. |

## Libraries

### Design system — Material 3 **1.5.0-alpha28**
| Option | Pros | Cons |
|---|---|---|
| **1.5.0-alpha (chosen)** | Full Material 3 Expressive: `MaterialExpressiveTheme`, `MaterialShapes`, expressive motion, button groups. This is the Bloom look. | Alpha. APIs can change. Needs opt-in annotations. |
| 1.4.0 stable (in BOM) | Stable. | Expressive components missing (`MaterialShapes` not in 1.4.0; checked in the AAR). |
| Custom design system | Full control. | Much more code; loses accessibility defaults. |

Only Material 3 is alpha. Everything else is stable.

### Navigation — Navigation 3 **1.1.7**
| Option | Pros | Cons |
|---|---|---|
| **Navigation 3 (chosen)** | New Compose-first API. You own the back stack as a list. Simple, testable. Stable. | Young; fewer examples. Tab back-stack handling is manual. |
| Navigation Compose 2.10.1 | Mature, type-safe routes. | Older NavController model; Google now points to Nav3 for new apps. |
| Voyager / Decompose | Multiplatform. | Third-party; not needed for Android-only. |

### Database — Room **3.0.3** (`androidx.room3`)
| Option | Pros | Cons |
|---|---|---|
| **Room 3 (chosen)** | Newest Room. Coroutines-first, driver-based, KSP-only, Flow queries. Stable. | New package names (`androidx.room3`); fewer docs and answers online. |
| Room 2.8.5 | Most documented. | Legacy SupportSQLite API; Room 3 is the future line. |
| SQLDelight 2.4.0 | SQL-first, multiplatform, very clear. | Third-party; more setup; no gain for Android-only. |
| DataStore only | No database. | Bad fit for event history and aggregate queries. |

SQLite driver: `AndroidSQLiteDriver` (system SQLite, 0 KB). Alternative `BundledSQLiteDriver` gives the same SQLite on all devices but adds about 1 MB native code per ABI.

### Settings — DataStore Preferences **1.2.1**
| Option | Pros | Cons |
|---|---|---|
| **Preferences DataStore (chosen)** | Async, Flow-based, safe. Official replacement for SharedPreferences. | Not type-safe per key (small risk; 5 keys). |
| Proto DataStore | Type-safe schema. | Needs protobuf plugin; overkill for 5 values. |
| SharedPreferences | Zero setup. | Blocking IO on main thread; deprecated in guidance. |

### Dependency injection — manual (`AppContainer`)
| Option | Pros | Cons |
|---|---|---|
| **Manual DI (chosen)** | No plugin, no codegen, plain Kotlin errors. About 7 dependencies and 6 consumers: small graph. | No compile-time graph check; wiring grows by hand. |
| Hilt 2.60.1 | Official. Compile-time safe. Built-in ViewModel/Service support. | Plugin + KSP build time; annotations everywhere; errors in generated code. |
| Metro 1.4.4 | Fast compiler plugin, modern, multiplatform. | Less Android glue out of the box. |
| Koin 4.2.2 | Simple DSL, no codegen. | Errors show at runtime. |

Revisit when the graph grows (more modules, many screens): Hilt or Metro.

### Async — Kotlin Coroutines **1.11.0** + Flow
No real alternative. RxJava is legacy for new Kotlin apps.

### Serialization — kotlinx.serialization **1.11.0**
Needed for Navigation 3 keys (saved back stack). Alternative: Parcelable keys — more code.

### Date and time — `java.time` (built in)
Alternative kotlinx-datetime 0.8.0: only useful for multiplatform. `java.time` is free on minSdk 31.

### Charts — custom Compose Canvas
Alternative Vico 3.3.1. The jar, pebbles and small daily bars are custom shapes; a chart library would fight the design and add size.

### Jar physics — dyn4j **6.0.0**
| Option | Pros | Cons |
|---|---|---|
| **dyn4j (chosen)** | Real shapes (pill, triangle, square, diamond), rotation, friction; natural feel; pure Java, no native code. | ~53 KB in the release APK; needs tuning for pixel-sized bodies. |
| Own circle engine | Tiny, fully controlled. | Circles only; pile felt bumpy and restless. |
| Box2D (via libGDX / JBox2D) | Well known. | Native libraries or an old, unmaintained port. |

Tuning: at-rest thresholds raised, damping ramps up 1.5–3.5 s after the last tilt change, solid 300 px walls,
max 25 px per step, restitution 0, friction 0.5. Full quality (120 Hz, 10/10 solver passes) at any pebble count:
at 511 pebbles it costs 3.1 ms per frame against 3.7 ms with 60 Hz, 6/4 passes (JVM on the host), and the pile is calmer.

### Fonts — bundled variable fonts (Bricolage Grotesque, Figtree; OFL)
| Option | Pros | Cons |
|---|---|---|
| **Bundled (chosen)** | Works offline, first frame correct, no Play Services dependency. | About 470 KB in the APK. |
| Downloadable Google Fonts | Smaller APK. | Needs Google Play Services; fallback font on first launch. |

### Testing
| Choice | Alternatives | Why |
|---|---|---|
| JUnit 4 + kotlin.test + coroutines-test | JUnit 5 (third-party Android plugin), Kotest | JUnit 4 is what AndroidX test tools support natively. Business rules (prompt policy, stats) are pure Kotlin, so plain unit tests cover them. |
| (later) Compose UI tests, Robolectric 4.17 | Espresso | Add when screens settle. |

## System integration

| Problem | Choice | Alternatives | Why |
|---|---|---|---|
| Detect unlock | Foreground service (`specialUse`) with a runtime `USER_PRESENT` / `SCREEN_OFF` receiver | Accessibility service; WorkManager polling | Manifest receivers do not get these broadcasts (Android 8+). Accessibility: heavy permission, strict Play review. Polling: late and battery-costly. |
| Show the question | Overlay window (`SYSTEM_ALERT_WINDOW`) with Compose inside | Start an Activity from the service | Android 15+ blocks background activity starts unless the app already shows an overlay window. The overlay works on all versions. |
| Restart after reboot | `BOOT_COMPLETED` receiver | — | `specialUse` services may start from boot (other types are blocked on Android 15). |
| App usage | Not read | UsageStatsManager | You chose low permissions. Session length comes from unlock → screen off. |
| Internet | None | — | Nothing needs it. The coffee link opens the browser. |
