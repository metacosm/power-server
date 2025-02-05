package net.laprun.sustainability.power.measure;

import java.time.Duration;

public record Timing(long[] timestamps, long startedAt, long samplePeriod) {
    public PartialCursor cursorOver(long timestamp, Duration duration) {
        return Cursor.cursorOver(timestamps, timestamp, duration, startedAt, samplePeriod);
    }
}
