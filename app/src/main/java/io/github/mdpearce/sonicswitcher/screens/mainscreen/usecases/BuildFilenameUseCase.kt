package io.github.mdpearce.sonicswitcher.screens.mainscreen.usecases

import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BuildFilenameUseCase
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd (H:mm:ss)")

        operator fun invoke(): String =
            "Switched ${formatter.format(clock.instant().atZone(ZoneId.systemDefault()))}.mp3"
    }
