# Sonic Switcher - Testing Strategy

## Executive Summary

This document outlines a comprehensive testing strategy for Sonic Switcher, covering unit tests, integration tests, and UI instrumentation tests. The approach prioritizes simplicity, effectiveness, and modern Android testing best practices.

**Current State**: No tests exist  
**Goal**: Achieve >80% code coverage with a pragmatic, maintainable test suite

---

## Testing Philosophy

### Principles
- **Test behavior, not implementation**: Focus on what code does, not how it does it
- **Pyramid structure**: Many unit tests, some integration tests, few UI tests
- **Fast feedback**: Unit tests run in <5 seconds, full suite in <30 seconds
- **Maintainability**: Tests should be simple, readable, and easy to update
- **Idiomatic Kotlin**: Leverage Kotlin features (coroutines, flows, Result types)

### Test Pyramid
```
         ┌────────┐
         │UI Tests│ ~10% (End-to-end critical paths)
         └────────┘
       ┌────────────┐
       │Integration │ ~20% (Module boundaries, FFmpeg)
       └────────────┘
    ┌─────────────────┐
    │   Unit Tests    │ ~70% (Business logic, ViewModels, Use Cases)
    └─────────────────┘
```

---

## Test Infrastructure

### Dependencies to Add

Update `gradle/libs.versions.toml`:

```toml
[versions]
# ... existing versions ...
turbine = "1.1.0"
mockk = "1.13.13"
coroutines-test = "1.9.0"
robolectric = "4.14.1"
compose-test = "1.7.5"

[libraries]
# ... existing libraries ...

# Testing
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines-test" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version.ref = "hilt" }
androidx-arch-core-testing = { module = "androidx.arch.core:core-testing", version = "2.2.0" }
```

### Test Source Sets

```
app/src/
├── main/           # Production code
├── test/           # JVM unit tests (fast, no Android framework)
├── testShared/     # Shared test utilities & fakes
└── androidTest/    # Instrumentation tests (on device/emulator)

converter/src/
├── main/
├── test/           # Converter unit tests
└── androidTest/    # Converter integration tests
```

---

## Unit Tests (70% of test suite)

### 1. ViewModel Tests

**Target**: `MainScreenViewModel`

**What to test**:
- State transitions (Empty → InputFileChosen → Processing → Complete)
- UI events emission (OpenFileChooser, OpenOutputFileChooser, etc.)
- Error handling (conversion failures, file copy errors)
- Queue management (add, clear, share multiple)
- Coroutine cancellation

**Tools**:
- **Turbine**: For testing Flows and StateFlows
- **MockK**: For mocking dependencies
- **kotlinx-coroutines-test**: For controlling coroutine execution

**Example test structure**:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var audioFileConverter: AudioFileConverter
    private lateinit var repository: ConvertedFileRepository
    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        audioFileConverter = mockk()
        repository = mockk(relaxed = true)
        // ... mock other dependencies
        
        viewModel = MainScreenViewModel(...)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onInputFileChosen updates state to InputFileChosen with display name`() = runTest {
        // Arrange
        val uri = Uri.parse("content://audio/123")
        every { getFileDisplayName(uri) } returns "test.mp3"

        // Act
        viewModel.onInputFileChosen(uri)

        // Assert
        viewModel.screenState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf<InputFileChosen>()
            assertThat((state as InputFileChosen).inputDisplayName).isEqualTo("test.mp3")
        }
    }

    @Test
    fun `onConvertClicked emits OpenOutputFileChooser event`() = runTest {
        // Arrange
        val inputUri = Uri.parse("content://audio/123")
        every { buildFilename() } returns "output.mp3"

        // Act
        viewModel.onConvertClicked(inputUri)

        // Assert
        viewModel.uiEvents.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf<OpenOutputFileChooser>()
            assertThat((event as OpenOutputFileChooser).defaultFilename).isEqualTo("output.mp3")
        }
    }

    @Test
    fun `conversion success transitions to Complete state`() = runTest {
        // Test happy path conversion
    }

    @Test
    fun `conversion failure transitions to Error state`() = runTest {
        // Test error handling
    }
}
```

**Coverage target**: >90% for ViewModel logic

---

### 2. Use Case Tests

**Targets**: All use cases in `screens/mainscreen/usecases/`

**What to test**:
- Business logic correctness
- Edge cases (null inputs, empty strings, invalid URIs)
- Error conditions
- Dependencies interactions

**Tools**:
- **MockK**: For mocking Android framework classes (ContentResolver, Clock)
- **JUnit 5** (optional): For better parameterized tests

**Example test structure**:

```kotlin
class GetFileDisplayNameUseCaseTest {
    private lateinit var contentResolver: ContentResolver
    private lateinit var context: Context
    private lateinit var useCase: GetFileDisplayNameUseCase

    @Before
    fun setup() {
        contentResolver = mockk()
        context = mockk()
        useCase = GetFileDisplayNameUseCase(context, contentResolver)
    }

    @Test
    fun `returns display name from content resolver for content URI`() {
        // Arrange
        val uri = Uri.parse("content://media/audio/123")
        val cursor = mockk<Cursor> {
            every { moveToFirst() } returns true
            every { getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
            every { getString(0) } returns "song.mp3"
            every { close() } just Runs
        }
        every { contentResolver.query(uri, null, null, null, null) } returns cursor

        // Act
        val result = useCase(uri)

        // Assert
        assertThat(result).isEqualTo("song.mp3")
        verify { cursor.close() }
    }

    @Test
    fun `returns unknown for null URI`() {
        val result = useCase(null)
        assertThat(result).isEqualTo("unknown")
    }

    @Test
    fun `falls back to path segments when content resolver fails`() {
        // Test fallback logic
    }
}
```

**Coverage target**: 100% for use cases (they're pure business logic)

---

### 3. Repository Tests

**Target**: `ConvertedFileRepository`

**What to test**:
- Flow transformations (entity → domain model)
- DAO interactions
- Suspend function behavior

**Tools**:
- **Turbine**: For testing Flows
- **Fake DAO**: In-memory implementation for testing

**Example**:

```kotlin
class ConvertedFileRepositoryTest {
    private lateinit var dao: FakeConvertedFileDao
    private lateinit var repository: ConvertedFileRepository

    @Before
    fun setup() {
        dao = FakeConvertedFileDao()
        repository = ConvertedFileRepository(dao)
    }

    @Test
    fun `getAllFiles maps entities to domain models`() = runTest {
        // Arrange
        dao.insertFile(ConvertedFileEntity(
            id = 1,
            uri = "content://test",
            displayName = "test.mp3",
            timestampMillis = 123456L
        ))

        // Act & Assert
        repository.getAllFiles().test {
            val files = awaitItem()
            assertThat(files).hasSize(1)
            assertThat(files[0].uri).isEqualTo(Uri.parse("content://test"))
        }
    }

    @Test
    fun `addFile inserts entity with correct data`() = runTest {
        // Act
        repository.addFile(
            uri = Uri.parse("content://audio/1"),
            displayName = "new.mp3",
            timestampMillis = 999L
        )

        // Assert
        val entities = dao.getAllFilesSnapshot()
        assertThat(entities).hasSize(1)
        assertThat(entities[0].displayName).isEqualTo("new.mp3")
    }
}
```

**Coverage target**: >90%

---

### 4. Sealed Class Tests

**Targets**: `ScreenState`, `UiEvents`, `ConversionResult`, `ProgressUpdate`

**What to test**:
- Data class equality
- Exhaustive when statements (compile-time check)

**Example**:

```kotlin
class ScreenStateTest {
    @Test
    fun `InputFileChosen equality works correctly`() {
        val uri = Uri.parse("content://test")
        val state1 = InputFileChosen(uri, "test.mp3")
        val state2 = InputFileChosen(uri, "test.mp3")
        val state3 = InputFileChosen(uri, "different.mp3")

        assertThat(state1).isEqualTo(state2)
        assertThat(state1).isNotEqualTo(state3)
    }

    @Test
    fun `all ScreenState types are handled in when expression`() {
        // Compile-time exhaustiveness check
        val states = listOf(
            Empty,
            InputFileChosen(Uri.EMPTY, "test"),
            Processing(Inactive),
            Complete(Uri.EMPTY),
            Error("error")
        )

        states.forEach { state ->
            val result = when (state) {
                is Empty -> "empty"
                is InputFileChosen -> "input"
                is Processing -> "processing"
                is Complete -> "complete"
                is Error -> "error"
            }
            assertThat(result).isNotNull()
        }
    }
}
```

**Coverage target**: 100% (trivial, but ensures data class contracts)

---

## Integration Tests (20% of test suite)

### 1. Converter Module Tests

**Target**: `FFMpegKitConverter`

**What to test**:
- Actual audio conversion with real FFmpegKit
- Progress callbacks
- Cancellation
- Error scenarios (invalid files, corrupted audio)

**Tools**:
- **Robolectric** or **Instrumentation tests**: FFmpegKit needs Android Context
- **Test assets**: Small audio files in various formats

**Example**:

```kotlin
@RunWith(AndroidJUnit4::class)
class FFMpegKitConverterTest {
    private lateinit var context: Context
    private lateinit var converter: FFMpegKitConverter

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        converter = FFMpegKitConverter(context)
    }

    @Test
    fun converts_wav_to_mp3_successfully() = runTest {
        // Arrange
        val inputUri = getTestAssetUri("test_audio.wav")
        val outputUri = createTempFileUri("output.mp3")
        val progressUpdates = mutableListOf<ProgressUpdate>()

        // Act
        val result = converter.convertAudioFile(
            input = inputUri,
            output = outputUri,
            onProgressUpdated = { progressUpdates.add(it) }
        )

        // Assert
        assertThat(result).isInstanceOf<ConversionComplete>()
        assertThat(progressUpdates).isNotEmpty()
        assertThat(progressUpdates.last()).isInstanceOf<Processing>()
        assertOutputFileExists(outputUri)
    }

    @Test
    fun conversion_fails_for_invalid_input() = runTest {
        // Test error handling
    }

    @Test
    fun conversion_can_be_cancelled() = runTest {
        // Test cancellation via coroutine context
    }
}
```

**Coverage target**: >70% (native code limits)

---

### 2. Database Tests

**Target**: Room DAO + Database

**What to test**:
- CRUD operations
- Flow emissions on data changes
- Query correctness
- Migration scenarios (future)

**Tools**:
- **In-memory Room database**

**Example**:

```kotlin
@RunWith(AndroidJUnit4::class)
class ConvertedFileDaoTest {
    private lateinit var database: SonicSwitcherDatabase
    private lateinit var dao: ConvertedFileDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SonicSwitcherDatabase::class.java
        ).build()
        dao = database.convertedFileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertFile_and_getAllFiles_returnsInDescendingOrder() = runTest {
        // Arrange
        val file1 = ConvertedFileEntity(uri = "uri1", displayName = "first", timestampMillis = 100)
        val file2 = ConvertedFileEntity(uri = "uri2", displayName = "second", timestampMillis = 200)

        // Act
        dao.insertFile(file1)
        dao.insertFile(file2)

        // Assert
        dao.getAllFiles().test {
            val files = awaitItem()
            assertThat(files[0].displayName).isEqualTo("second") // Newer first
            assertThat(files[1].displayName).isEqualTo("first")
        }
    }

    @Test
    fun clearAll_removesAllFiles() = runTest {
        // Test data deletion
    }

    @Test
    fun getCount_returnsCorrectCount() = runTest {
        // Test count query
    }
}
```

**Coverage target**: 100% for DAO methods

---

## UI Tests (10% of test suite)

### Compose UI Tests

**Target**: `MainScreen` composable

**What to test**:
- Critical user journeys (end-to-end flows)
- UI state rendering
- User interactions trigger correct ViewModel calls
- Accessibility (semantic properties)

**Tools**:
- **Compose Test (androidx.compose.ui.test)**
- **Hilt testing** for injecting fakes

**Example**:

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun emptyState_showsSelectFileMessage() {
        composeTestRule.onNodeWithText("Please select a file to begin")
            .assertIsDisplayed()
    }

    @Test
    fun clickChooseFileButton_opensFilePicker() {
        // Use Intents.intended() to verify ACTION_OPEN_DOCUMENT
        composeTestRule.onNodeWithText("Choose file").performClick()
        // Assert file picker intent was launched (requires Espresso Intents)
    }

    @Test
    fun completeState_showsShareAndAddToQueueButtons() {
        // Set ViewModel state to Complete
        composeTestRule.setContent {
            MainScreenContent(
                onOpenFileChooserClicked = {},
                onConvertClicked = {},
                onShareClicked = {},
                onAddToQueueClicked = {},
                onShareAllQueuedClicked = {},
                onClearQueueClicked = {},
                screenState = Complete(Uri.parse("content://test")),
                queuedFiles = emptyList(),
                queueCount = 0
            )
        }

        composeTestRule.onNodeWithText("Share file").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add to queue").assertIsDisplayed()
    }

    @Test
    fun queueSection_displaysFilesCorrectly() {
        val mockFiles = listOf(
            ConvertedFile(1, Uri.parse("uri1"), "file1.mp3", 100),
            ConvertedFile(2, Uri.parse("uri2"), "file2.mp3", 200)
        )

        composeTestRule.setContent {
            QueueSection(
                queuedFiles = mockFiles,
                queueCount = 2,
                onShareAllClicked = {},
                onClearQueueClicked = {}
            )
        }

        composeTestRule.onNodeWithText("file1.mp3").assertIsDisplayed()
        composeTestRule.onNodeWithText("file2.mp3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Queue (2 files)").assertIsDisplayed()
    }

    @Test
    fun accessibility_allButtonsHaveContentDescriptions() {
        // Verify semantic properties for screen readers
    }
}
```

**Coverage target**: Critical paths only (~5-10 tests)

---

## Test Doubles Strategy

### Fakes over Mocks (when possible)

**Fakes to create**:

1. **FakeConvertedFileDao**: In-memory list implementation
```kotlin
class FakeConvertedFileDao : ConvertedFileDao {
    private val files = mutableListOf<ConvertedFileEntity>()
    private val _filesFlow = MutableStateFlow(files.toList())

    override fun getAllFiles(): Flow<List<ConvertedFileEntity>> = _filesFlow

    override suspend fun insertFile(file: ConvertedFileEntity) {
        files.add(file.copy(id = files.size.toLong() + 1))
        _filesFlow.value = files.toList()
    }

    override suspend fun clearAll() {
        files.clear()
        _filesFlow.value = emptyList()
    }

    override fun getCount(): Flow<Int> = _filesFlow.map { it.size }

    fun getAllFilesSnapshot() = files.toList() // For test assertions
}
```

2. **FakeAudioFileConverter**: Simulates conversion without FFmpeg
```kotlin
class FakeAudioFileConverter : AudioFileConverter {
    var shouldSucceed = true
    var conversionDelayMs = 100L

    override suspend fun convertAudioFile(
        input: Uri,
        output: Uri,
        onProgressUpdated: (ProgressUpdate) -> Unit
    ): ConversionResult {
        onProgressUpdated(Processing(0.0f))
        delay(conversionDelayMs / 2)
        onProgressUpdated(Processing(0.5f))
        delay(conversionDelayMs / 2)
        onProgressUpdated(Inactive)

        return if (shouldSucceed) {
            ConversionComplete
        } else {
            throw ConversionException("Test failure")
        }
    }
}
```

3. **TestClock**: Controllable time for filename generation
```kotlin
class TestClock(private var instant: Instant = Instant.EPOCH) : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun instant(): Instant = instant
    override fun withZone(zone: ZoneId): Clock = this

    fun setInstant(newInstant: Instant) {
        instant = newInstant
    }
}
```

---

## Continuous Integration

### GitHub Actions Workflow

See `.github/workflows/pr-checks.yml` for the full implementation.

**Overview:**
```yaml
name: PR Checks

on:
  pull_request:
    branches: [ main ]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew ktlintCheck

  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew test --continue

  coverage:
    runs-on: ubuntu-latest
    needs: [unit-tests]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew koverXmlReport koverHtmlReport
      - uses: madrapps/jacoco-report@v1.7.1  # Works with Kover XML
        with:
          paths: build/reports/kover/report.xml
          min-coverage-overall: 60

  pr-check-summary:
    needs: [lint, unit-tests, coverage]
    runs-on: ubuntu-latest
    steps:
      - run: echo "All checks passed!"
```

**Note**: Instrumentation tests are not run in PR workflow to keep costs free/low. They can be added to a separate workflow that runs on-demand or for release branches.

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Add test dependencies to `gradle/libs.versions.toml`
- [ ] Set up test source sets
- [ ] Create `testShared` module with fakes
- [ ] Write first ViewModel test as template

### Phase 2: Core Tests (Week 2-3)
- [ ] Complete ViewModel tests (all state transitions)
- [ ] Complete Use Case tests (all 5 use cases)
- [ ] Complete Repository tests
- [ ] Achieve >70% unit test coverage

### Phase 3: Integration (Week 4)
- [ ] Converter integration tests (FFmpeg)
- [ ] Room DAO tests
- [ ] Achieve >80% overall coverage

### Phase 4: UI Tests (Week 5)
- [ ] Critical path Compose UI tests
- [ ] Accessibility tests
- [ ] Screenshot tests (optional)

### Phase 5: CI/CD (Week 6) ✅
- [x] GitHub Actions workflow for PR checks
- [x] Kover coverage reporting (Kotlin-native)
- [x] PR gates (lint + tests must pass)
- [x] Automated coverage comments on PRs

**Status**: ✅ **IMPLEMENTED**

#### GitHub Actions Workflow

The project uses GitHub Actions for automated PR validation. The workflow includes:

**Jobs:**
1. **Lint Check** - Runs `ktlintCheck` on all modules
2. **Unit Tests** - Executes all unit tests (`./gradlew test`)
3. **Coverage** - Generates Kover reports and posts coverage to PR comments
4. **PR Check Summary** - Consolidates all check results

**Features:**
- Free public runners (ubuntu-latest)
- Gradle dependency caching for faster builds
- Artifact uploads (test reports, coverage reports)
- Concurrent run cancellation (saves resources)
- Dummy `google-services.json` for CI (Firebase not needed for tests)

**Required Check**: Set `PR Check Summary` as a required status check in GitHub branch protection rules

#### Kover Configuration

Uses **Kover 0.9.0** (modern Kotlin-native coverage tool) instead of JaCoCo:
- Multi-module aggregated reporting (`:app` + `:converter`)
- Excludes generated code (Hilt, R classes, BuildConfig, etc.)
- Generates XML (for CI) and HTML (for human viewing) reports
- Minimum coverage threshold: 60% (will increase as tests are added)

**Commands:**
```bash
./gradlew test koverXmlReport koverHtmlReport  # Generate all reports
./gradlew koverLog                              # Print coverage to console
./gradlew koverVerify                           # Enforce coverage thresholds
```

**Report Locations:**
- XML: `build/reports/kover/report.xml`
- HTML: `build/reports/kover/html/index.html`

#### Local Testing

Validate CI/CD locally before pushing:
```bash
# Run all checks (same as CI)
./gradlew ktlintCheck test koverXmlReport koverHtmlReport

# Quick checks
./gradlew ktlintCheck    # Lint only
./gradlew test           # Tests only
./gradlew koverLog       # Coverage summary
```

#### Cost Optimization

- Uses public GitHub-hosted runners (free for public repos)
- Gradle caching reduces build times (30-60% faster after first run)
- Concurrent run cancellation prevents wasted resources
- No instrumentation tests in PR workflow (requires emulator, slower)

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Unit test coverage | >80% |
| Integration test coverage | >70% |
| UI test coverage | Critical paths only |
| Test execution time (unit) | <10 seconds |
| Test execution time (full suite) | <5 minutes |
| Flaky test rate | <2% |
| Tests per PR | >1 test per new feature |

---

## Best Practices & Gotchas

### Do's ✅
- Use `runTest` for all coroutine tests (handles test dispatcher)
- Use Turbine for Flow assertions (cleaner than `first()`, `toList()`)
- Prefer fakes over mocks for complex dependencies
- Test public API, not private implementation
- Use descriptive test names: `function_scenario_expectedBehavior()`
- Group related tests with nested classes

### Don'ts ❌
- Don't test Android framework code (assume it works)
- Don't use `Thread.sleep()` in tests (use `runTest` + virtual time)
- Don't make tests depend on each other (isolation)
- Don't test generated code (Hilt, Room, Compose)
- Don't mock value classes (Uri, File) - use real instances

### Common Pitfalls

1. **Flow testing without Turbine**: Manual collection is verbose
   ```kotlin
   // ❌ Hard to test
   val result = flow.first()
   
   // ✅ Turbine makes it readable
   flow.test {
       assertThat(awaitItem()).isEqualTo(expected)
   }
   ```

2. **Not using test dispatcher**: Leads to flaky timing issues
   ```kotlin
   // ✅ Always use runTest
   @Test
   fun myTest() = runTest {
       // coroutine code here
   }
   ```

3. **Over-mocking**: Creates brittle tests
   ```kotlin
   // ❌ Too many mocks
   every { dep1.foo() } returns bar
   every { dep2.baz() } returns qux
   // ... 10 more lines
   
   // ✅ Use a fake
   val fakeDep = FakeDependency()
   ```

---

## Resources

- [Testing in Android](https://developer.android.com/training/testing)
- [Turbine Documentation](https://github.com/cashapp/turbine)
- [MockK Documentation](https://mockk.io/)
- [Compose Testing Guide](https://developer.android.com/jetpack/compose/testing)
- [Hilt Testing Guide](https://developer.android.com/training/dependency-injection/hilt-testing)

---

## Appendix: Example Test File Structure

```
app/src/test/kotlin/io/github/mdpearce/sonicswitcher/
├── screens/
│   └── mainscreen/
│       ├── MainScreenViewModelTest.kt
│       ├── usecases/
│       │   ├── GetFileDisplayNameUseCaseTest.kt
│       │   ├── BuildFilenameUseCaseTest.kt
│       │   ├── CopyInputFileToTempDirectoryUseCaseTest.kt
│       │   ├── AddFileToQueueUseCaseTest.kt
│       │   └── ClearQueueUseCaseTest.kt
│       └── ScreenStateTest.kt
├── data/
│   └── ConvertedFileRepositoryTest.kt
└── testutil/
    ├── fakes/
    │   ├── FakeConvertedFileDao.kt
    │   ├── FakeAudioFileConverter.kt
    │   └── TestClock.kt
    └── TestExtensions.kt

app/src/androidTest/kotlin/io/github/mdpearce/sonicswitcher/
├── screens/mainscreen/MainScreenTest.kt
├── data/ConvertedFileDaoTest.kt
└── testutil/HiltTestRunner.kt

converter/src/test/kotlin/io/github/mdpearce/sonicswitcher/converter/
└── AudioFileConverterTest.kt (if possible without Android)

converter/src/androidTest/kotlin/io/github/mdpearce/sonicswitcher/converter/
└── FFMpegKitConverterTest.kt
```
