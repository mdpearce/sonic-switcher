package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import com.google.common.truth.Truth.assertThat
import io.github.mdpearce.sonicswitcher.testutil.fakes.TestClock
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BuildFilenameUseCaseTest {
    private lateinit var clock: TestClock
    private lateinit var useCase: BuildFilenameUseCase

    @Before
    fun setup() {
        clock = TestClock()
        useCase = BuildFilenameUseCase(clock)
    }

    @Test
    fun `builds filename with formatted timestamp`() {
        // Arrange
        val instant = Instant.parse("2024-03-15T14:30:45Z")
        clock.setInstant(instant)

        // Act
        val result = useCase()

        // Assert
        // Format: "Switched yyyy-MM-dd (H:mm:ss).mp3"
        // Date will be in system timezone, so check for structure rather than specific date
        assertThat(result).startsWith("Switched ")
        assertThat(result).contains("2024-")
        assertThat(result).contains("(")
        assertThat(result).contains(":")
        assertThat(result).contains(")")
        assertThat(result).endsWith(".mp3")
    }

    @Test
    fun `filename format matches expected pattern`() {
        // Arrange
        clock.setMillis(1710513045000L) // 2024-03-15T14:30:45Z

        // Act
        val result = useCase()

        // Assert
        assertThat(result.matches(Regex("Switched \\d{4}-\\d{2}-\\d{2} \\(\\d{1,2}:\\d{2}:\\d{2}\\)\\.mp3"))).isTrue()
    }

    @Test
    fun `different times produce different filenames`() {
        // Arrange & Act
        clock.setInstant(Instant.parse("2024-03-15T10:00:00Z"))
        val filename1 = useCase()

        clock.setInstant(Instant.parse("2024-03-15T16:00:00Z"))
        val filename2 = useCase()

        // Assert
        assertThat(filename1).isNotEqualTo(filename2)
    }

    @Test
    fun `filename at midnight uses correct format`() {
        // Arrange
        clock.setInstant(Instant.parse("2024-01-01T00:00:00Z"))

        // Act
        val result = useCase()

        // Assert
        // Format should handle midnight correctly with single-digit hour
        assertThat(result).contains("2024-01-01")
        assertThat(result.matches(Regex("Switched \\d{4}-\\d{2}-\\d{2} \\(\\d{1,2}:\\d{2}:\\d{2}\\)\\.mp3"))).isTrue()
    }

    @Test
    fun `filename at noon uses correct format`() {
        // Arrange
        clock.setInstant(Instant.parse("2024-06-15T12:00:00Z"))

        // Act
        val result = useCase()

        // Assert
        assertThat(result).contains("2024-06-15")
        // Time portion will vary based on system timezone
        assertThat(result).endsWith(".mp3")
    }

    @Test
    fun `filename uses system default timezone`() {
        // Arrange
        val instant = Instant.parse("2024-12-25T23:59:59Z")
        clock.setInstant(instant)

        // Act
        val result = useCase()

        // Assert
        val expectedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
        assertThat(result).contains(expectedDate)
    }
}
