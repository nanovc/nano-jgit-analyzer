package io.nanovc.nano_jgit_analyzer;

import io.nanovc.ClockBase;
import io.nanovc.timestamps.InstantTimestamp;

import java.time.Instant;

/**
 * This is a simulated clock that allows us to override timestamps.
 */
public class SimulatedInstantClock extends ClockBase<InstantTimestamp>
{
    /**
     * The override value to use for the current instant in time.
     */
    public Instant nowOverride = Instant.now();

    /**
     * Creates a timestamp for the current instant in time.
     *
     * @return A new timestamp for the current instant in time.
     */
    @Override public InstantTimestamp now()
    {
        return new InstantTimestamp(this.nowOverride);
    }
}
