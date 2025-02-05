package net.laprun.sustainability.power.measure;

import java.time.Duration;

public enum Cursors {
    ;

    public static Cursor cursorOver(long[] timestamps, long timestamp, Duration duration, long initialOffset,
            long averagePeriodHint) {
        // adjusted timestamp for modding
        final var timestampForDiv = timestamp - initialOffset;
        final var durationAsMs = duration.toMillis();

        // cannot find an interval for a timestamp that is before the recording started
        if (timestampForDiv < 0) {
            return Cursor.empty;
        }

        if (timestamps.length < 2) {
            // if we don't have a sample period, use the full measure
            double ratio = 1.0;
            if (averagePeriodHint > 0) {
                ratio = (double) durationAsMs / averagePeriodHint;
            }
            return new Cursor(0, 0, ratio, ratio);
        }

        // estimate sample period based on 2 samples interval
        if (averagePeriodHint <= 0) {
            averagePeriodHint = timestamps[1] - timestamps[0];
        }

        // first, find potential first sample based on timestamp
        int startIndex = (int) Math.floorDiv(timestampForDiv, averagePeriodHint);
        int endIndex = (int) Math.floorDiv(timestampForDiv + durationAsMs, averagePeriodHint);

        if (startIndex == endIndex) {
            final long previousTimestamp = startIndex == 0 ? initialOffset : timestamps[startIndex - 1];
            final long slotDuration = timestamps[startIndex] - previousTimestamp;
            var ratio = (double) durationAsMs / slotDuration;
            return new Cursor(startIndex, endIndex, ratio, -1);
        }

        // get the index with the timestamp right after the one we're looking for since what we're interested in is the portion of the measure that gets recorded after the timestamp we want
        long afterTimestamp = timestamps[startIndex];
        final long startOffset = afterTimestamp - timestamp;
        double startRatio = 0;
        if (startOffset > 0) {
            startRatio = (double) startOffset / (afterTimestamp - timestamps[startIndex - 1]);
        }

        // look for the index that records the first timestamp that's after the one we're looking for added to the duration
        afterTimestamp = timestamps[endIndex];
        final long slotDuration = afterTimestamp - timestamps[endIndex - 1];
        final long endOffset = slotDuration - (afterTimestamp - timestamp - durationAsMs);
        double endRatio = 0;
        if (endOffset > 0) {
            endRatio = (double) endOffset / slotDuration;
        }

        return new Cursor(startIndex, endIndex, startRatio, endRatio);
    }
}
