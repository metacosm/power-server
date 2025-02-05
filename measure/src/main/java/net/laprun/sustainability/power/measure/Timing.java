package net.laprun.sustainability.power.measure;

import java.time.Duration;

public record Timing(long[] timestamps, long startedAt, long samplePeriod) {
    public Cursor cursorOver(long timestamp, Duration duration) {
        return Cursors.cursorOver(timestamps, timestamp, duration, startedAt, samplePeriod);
    }
}
