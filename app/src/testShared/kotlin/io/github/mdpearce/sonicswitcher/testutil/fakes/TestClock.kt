package io.github.mdpearce.sonicswitcher.testutil.fakes

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Test implementation of [Clock] that allows time to be controlled in tests.
 * Useful for testing time-dependent logic like filename generation.
 */
class TestClock(
    private var instant: Instant = Instant.EPOCH,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun instant(): Instant = instant

    override fun withZone(zone: ZoneId): Clock = TestClock(instant, zone)

    /**
     * Sets the current instant to the specified value.
     */
    fun setInstant(newInstant: Instant) {
        instant = newInstant
    }

    /**
     * Sets the current time in milliseconds since epoch.
     */
    fun setMillis(millis: Long) {
        instant = Instant.ofEpochMilli(millis)
    }

    /**
     * Advances the clock by the specified duration in milliseconds.
     */
    fun advanceBy(millis: Long) {
        instant = instant.plusMillis(millis)
    }

    /**
     * Resets the clock to epoch (1970-01-01T00:00:00Z).
     */
    fun reset() {
        instant = Instant.EPOCH
    }
}
