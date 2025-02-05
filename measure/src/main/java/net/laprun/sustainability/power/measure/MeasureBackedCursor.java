package net.laprun.sustainability.power.measure;

import net.laprun.sustainability.power.analysis.Recorder;

public record MeasureBackedCursor(Recorder measure, Cursor cursor) {
    public double sum() {
        return cursor.sum(measure.liveMeasures());
    }

    public double[] view() {
        return cursor.viewOf(measure.liveMeasures());
    }
}
