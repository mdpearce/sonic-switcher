package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import io.github.mdpearce.sonicswitcher.data.ConvertedFileRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ClearQueueUseCaseTest {
    private lateinit var repository: ConvertedFileRepository
    private lateinit var useCase: ClearQueueUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = ClearQueueUseCase(repository)
    }

    @Test
    fun `invokes repository clearAll`() =
        runTest {
            // Act
            useCase()

            // Assert
            coVerify(exactly = 1) { repository.clearAll() }
        }

    @Test
    fun `multiple invocations call repository multiple times`() =
        runTest {
            // Act
            useCase()
            useCase()
            useCase()

            // Assert
            coVerify(exactly = 3) { repository.clearAll() }
        }
}
