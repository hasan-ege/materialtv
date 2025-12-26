## 7. Recent Polish & Improvements
- [x] Refined documentation: Moved Turkish translation to `READMETR.md` and added prominent liability disclaimer to both versions.
- [x] Removed subtitle size selection from Settings (simplified UI).
- [x] Added "Current Features" section to Settings for better user onboarding and info.
- [x] Optimized Settings UI with Material 3 Expressive components and smoother animations.
- [x] Refactored hardcoded strings in Favorites sorting menu and badges for better localization.
- [x] Redesigned Favorites sorting menu with a premium, expressive UI and removed "Custom Order" as requested.
- [x] Unified Filter and Sort dialogs with a shared expressive design, including icons, haptics, and sort direction control.

# MaterialTV Deficiencies & Improvements
...

## 1. Hardcoded Strings (Localization needed)
Strings that are not in `res/strings.xml` and should be moved there for better localization support.

- ~~`PlayerActivity.kt`: "10s", "Close", "Audio Tracks", "No audio tracks available", "Subtitles", "No subtitle tracks available", "Audio & Subtitle Selection", "Engine: ...", "Position: ..."~~
- ~~`HomeActivity.kt`: "All", "See All", "Added to favorites", "Removed from favorites", "No series found", "No live streams found"~~
- ~~`DetailScreen.kt`: "Season"~~
- ~~`DownloadNotificationHelper.kt`: Turkish strings like "Hata", "Tekrar denemek için dokunun", "tamamlandı", "Devam etmek için dokunun"~~
- ~~`MainActivity.kt`: Various debug logs and potentially some UI messages.~~

## 2. Null Safety Risks (Usage of `!!`)
Places where the force-unwrap operator is used, which can cause `NullPointerException`.

- ~~`PlayerActivity.kt#L647`: seriesData.episodes!!~~
- ~~`HomeActivity.kt#L812`: item.episodeId!!~~
- ~~`MainActivity.kt#L113`: serverUrl!!, username!!, password!!~~

## 3. Error Handling
- ~~Several `try-catch` blocks only use `Log.e` without providing feedback to the user or attempting a recovery.~~
- Network errors in `M3uRepository` and `OkHttpDownloader` could be more robust.

## 4. Material 3 Expressive Implementation
- ~~**Shapes**: Some dialogs and cards use standard shapes instead of the custom `ExpressiveShapes` defined in the theme.~~
- ~~**Animations**: Standard `tween` animations are used in some places where `spring` (bouncy) or `ExpressiveAnimations` should be used.~~
- **Colors**: Hardcoded colors (e.g., `Color.Black.copy(alpha = 0.7f)`) should be replaced with theme-aware colors from `MaterialTheme.colorScheme`.

## 5. Potential Refactoring & Optimizations
- `PlayerActivity.kt`: At 2300+ lines, it's becoming difficult to maintain. Logic could be moved to ViewModels or separate managers.
- `ContentRow` & `SeriesContentRow`: Significant code duplication between different content row types.
- `M3uParser.kt`: Could potentially be optimized for faster parsing of large playlists.

## 6. User Rules & Memory Compliance
- Ensuring all icons have proper `contentDescription`.
- Verifying all interactive elements have spring-based scale animations.
- Ensuring `DIRECTORY_DOWNLOADS` is consistently used.
