# Sonic Switcher - AI Coding Agent Instructions

## Project Overview
Android audio converter app (MP3 output) built with Jetpack Compose, Kotlin Coroutines, Hilt DI, and FFmpegKit. Two-module architecture: `:app` (UI/interactions) and `:converter` (audio conversion logic).

**Quality Standards**: This is a public open-source project. All code contributions must follow modern Android best practices, including proper testing, documentation, and adherence to Material Design guidelines.

## Architecture Patterns

### Module Structure
- **`:app`**: UI layer with Compose screens, ViewModels, and use cases. Depends on `:converter`.
- **`:converter`**: Self-contained library module exposing `AudioFileConverter` interface, implemented by `FFMpegKitConverter`. No dependencies on `:app`.

### State Management
- **Sealed classes** for type-safe state modeling (see `ScreenState.kt`, `UiEvents.kt`, `ConversionResult.kt`, `ProgressUpdate.kt`)
- **StateFlow** for screen state, **SharedFlow** for one-time UI events
- Example: `MainScreenViewModel` uses `MutableStateFlow<ScreenState>` with states: `Empty`, `InputFileChosen`, `Processing`, `Complete`, `Error`

### Dependency Injection (Hilt)
- All modules use `@HiltViewModel` for ViewModels, `@Inject` constructor injection for use cases
- Hilt modules in `di/` packages: `MainScreenModule` (provides `ContentResolver`, `Clock`, cache dir), `ConverterModule` (provides `AudioFileConverter`), `FirebaseModule`
- Named qualifiers for ambiguous deps: `@Named(MainScreenModule.CACHE_DIR)`

### Use Case Pattern
- Business logic extracted to operator invoke classes: `GetFileDisplayNameUseCase`, `BuildFilenameUseCase`, `CopyInputFileToTempDirectoryUseCase`
- Located in `screens/mainscreen/usecases/`
- Constructor injection with `@Inject`, called via `operator fun invoke(...)`

### File Handling
- Uses Android Storage Access Framework (SAF) URIs throughout
- `FileProvider` configured for sharing temp files (authority: `io.github.mdpearce.sonicswitcher.FileProvider`)
- Input files copied to cache dir before conversion to ensure FFmpeg can access them
- `FFmpegKitConfig.getSafParameterForRead/Write()` wraps URIs for FFmpeg

## Key Workflows

### Build & Run
```bash
./gradlew clean assembleDebug         # Build debug APK
./gradlew installDebug                # Install on device/emulator
./gradlew :app:assembleRelease        # Build release with ProGuard
```

### Code Quality
```bash
./gradlew ktlintCheck                 # Lint all modules (ktlint applied to subprojects)
./gradlew ktlintFormat                # Auto-format code
```

### Dependencies
- Version catalog in `gradle/libs.versions.toml` - use `libs.` references
- FFmpegKit audio variant: `libs.ffmpeg.kit.audio` (2.1.0)
- Compose BOM ensures version alignment for Compose artifacts

## Conventions

### Package Structure
```
io.github.mdpearce.sonicswitcher/
├── MainActivity (entry point, handles share intents)
├── SonicSwitcherApplication (@HiltAndroidApp)
├── screens/mainscreen/
│   ├── MainScreen.kt (@Composable UI)
│   ├── MainScreenViewModel.kt (state + events)
│   ├── ScreenState.kt, UiEvents.kt (sealed classes)
│   ├── usecases/ (business logic)
│   ├── models/ (data classes like FileWithUri)
│   └── errors/ (custom exceptions)
├── di/ (Hilt modules)
└── ui/theme/ (Material3 theming)
```

### Compose Patterns
- State hoisting: ViewModels emit state, screens are stateless
- Preview functions for all states: `@Preview(showBackground = true, name = "...")`
- Material3 design system via `AppTheme` wrapper
- Extensive use of Material Icons Extended for UI

### Coroutines & Threading
- `viewModelScope.launch` for UI-triggered operations
- `Dispatchers.IO` for file operations and conversion (see `onOutputFileChosen`)
- Suspending functions in converter use `suspendCancellableCoroutine` for FFmpeg callbacks

### Error Handling
- Custom exceptions: `FileCopyException`, `ConversionException`
- Sealed `ConversionResult`: `ConversionComplete`, `ConversionCancelled`, `ConversionError`
- ViewModel catches conversion errors and maps to `ScreenState.Error`
- **Best Practice**: Use Kotlin Result type (already dependency: `libs.kotlin.result`) for typed error handling in new code

### Testing (Needs Expansion)
- **Current State**: No unit or instrumentation tests exist
- **Required for new features**: 
  - ViewModels: Test state transitions with Turbine or similar
  - Use cases: Unit test business logic with mocked dependencies
  - Compose UI: Screenshot tests or semantics-based UI tests
  - Converter: Integration tests for FFmpeg operations
- **Setup needed**: Add test dependencies (Turbine, MockK, Compose test, Robolectric)

### Manifest Integrations
- Handles `ACTION_SEND` intents with `audio/*` MIME type (share from other apps)
- `MainActivity.onCreate` checks `sharedInputUri` from intent extras

## Firebase Integration
- Analytics and Crashlytics enabled (see `google-services.json`, `FirebaseModule`)
- Release builds have Crashlytics reports in `app/build/crashlytics/`

## Critical Files
- `app/build.gradle.kts`: Main dependencies, ProGuard rules, version metadata
- `gradle/libs.versions.toml`: Single source of truth for versions
- `converter/src/main/java/.../FFMpegKitConverter.kt`: Core conversion logic with progress callbacks
- `app/src/main/java/.../MainScreenViewModel.kt`: Orchestrates conversion flow
- `app/src/main/AndroidManifest.xml`: FileProvider config, share intent filters

## Common Tasks
- **Add new screen**: Create sealed `ScreenState`, update ViewModel, add `@Composable` with previews, **write tests**
- **New use case**: Create class in `usecases/`, inject deps, implement `operator fun invoke`, **write unit tests**
- **Update FFmpeg params**: Modify command string in `FFMpegKitConverter.executeAsync` (currently: `-acodec libmp3lame -b:a 256k`)
- **New dependency**: Add to `libs.versions.toml`, reference as `libs.dependency.name`

## Best Practices & Refactoring Opportunities

### Code Quality
- **Ktlint enforcement**: All code must pass `./gradlew ktlintCheck` before committing
- **Null safety**: Use `checkNotNull`, `requireNotNull`, and safe calls consistently
- **Immutability**: Prefer `val` over `var`, use immutable collections
- **Documentation**: Add KDoc comments for public APIs, especially in `:converter` module

### Architecture Improvements
- **Repository pattern**: Consider adding Repository layer between ViewModels and converter for caching/offline support
- **Unidirectional data flow**: Current implementation is good, maintain this pattern for new features
- **Domain models**: Keep Android-specific types (Uri, Context) out of `:converter` where possible
- **State restoration**: Ensure `ScreenState` survives process death (SavedStateHandle)

### UI/UX Best Practices
- **Accessibility**: Add content descriptions to all icons and images
- **Loading states**: Show shimmer or skeleton screens during processing
- **Error recovery**: Provide actionable error messages with retry options
- **Permissions**: Handle runtime permissions gracefully (even though SAF doesn't require them)
- **Dark mode**: Verify theme support in all screens (Material3 should handle this)

### Performance
- **Memory leaks**: Ensure coroutines are cancelled (viewModelScope handles this)
- **Large files**: Stream file copies instead of loading into memory (currently uses 8KB buffer - good)
- **Background work**: Consider WorkManager for conversions that outlive app lifecycle
- **ProGuard**: Verify R8 rules for FFmpeg keep necessary native symbols

### Security & Privacy
- **File cleanup**: Ensure temp files in cache dir are deleted after use (currently done in ViewModel)
- **URI validation**: Validate incoming share URIs to prevent malicious inputs
- **Permissions**: Document why `AD_ID` permission is needed (Firebase requirement)

### Testing Checklist (for new features)
- [ ] Unit tests for ViewModels (state transitions)
- [ ] Unit tests for use cases (business logic)
- [ ] UI tests for Compose screens (critical paths)
- [ ] Integration tests for FFmpeg converter
- [ ] Manual testing on different Android versions (minSdk 29+)
- [ ] Test with various audio formats (MP3, FLAC, WAV, M4A, etc.)
- [ ] ProGuard build verification

### Documentation Requirements
- **README**: Keep updated with new features and build instructions
- **KDoc**: Public APIs must have documentation
- **Code comments**: Explain "why" not "what" for complex logic
- **Changelog**: Maintain for version releases
