package net.laprun.sustainability.power.measure;

import java.time.Duration;

public enum Cursor {
    ;

    public static PartialCursor cursorOver(long[] timestamps, long timestamp, Duration duration, long initialOffset,
            long averagePeriodHint) {
        // adjusted timestamp for modding
        final var timestampForDiv = timestamp - initialOffset;
        final var durationAsMs = duration.toMillis();

        // cannot find an interval for a timestamp that is before the recording started
        if (timestampForDiv < 0) {
            return PartialCursor.empty;
        }

        if (timestamps.length < 2) {
            // if we don't have a sample period, use the full measure
            double ratio = 1.0;
            if (averagePeriodHint > 0) {
                ratio = (double) durationAsMs / averagePeriodHint;
            }
            return new PartialCursor(0, 0, ratio, ratio);
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
            return new PartialCursor(startIndex, endIndex, ratio, -1);
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

        return new PartialCursor(startIndex, endIndex, startRatio, endRatio);
    }

    public record PartialCursor(int startIndex, int endIndex, double firstMeasureRatio, double lastMeasureRatio) {

        public static final PartialCursor empty = new PartialCursor(-1, -1, 0.0, 0.0);

        public double sum(double[] values) {
            if (values == null || values.length == 0 || this == empty || values.length < startIndex + endIndex) {
                return 0.0;
            }

            if (startIndex == endIndex) {
                return values[startIndex] * firstMeasureRatio;
            }

            double sum = values[startIndex] * firstMeasureRatio;
            for (int i = startIndex + 1; i < endIndex; i++) {
                sum += values[i];
            }
            sum += values[endIndex] * lastMeasureRatio;

            return sum;
        }

        public double[] viewOf(double[] values) {
            if (values == null || values.length == 0 || this == empty || values.length < startIndex + endIndex) {
                return new double[0];
            }

            if (startIndex == endIndex) {
                return new double[] { values[startIndex] * firstMeasureRatio };
            }

            final int len = endIndex - startIndex + 1;
            final double[] view = new double[len];
            view[0] = values[startIndex] * firstMeasureRatio;
            System.arraycopy(values, startIndex + 1, view, 1, len - 1 - 1);
            view[len - 1] = values[endIndex] * lastMeasureRatio;
            return view;
        }
    }
}
