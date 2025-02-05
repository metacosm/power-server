package net.laprun.sustainability.power.measure;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CursorsTest {

    @ParameterizedTest
    @ValueSource(longs = { -1, 0, 100, 98, 105 })
    void cursorOverSimple(long periodHint) {
        final var timestamps = new long[] { 100, 200, 300, 400, 500, 600, 700, 800, 900 };

        final var cursor = Cursors.cursorOver(timestamps, 225, Duration.ofMillis(540 - 225), 0,
                periodHint);

        assertEquals(2, cursor.startIndex());
        assertEquals(5, cursor.endIndex());
        assertEquals(0.75, cursor.firstMeasureRatio(), 0.0001);
        assertEquals(0.4, cursor.lastMeasureRatio(), 0.0001);

        final var measures = new double[] { 100, 200, 300, 400, 500, 600, 700, 800, 900 };
        assertEquals(300 * 0.75 + 400 + 500 + 0.4 * 600, cursor.sum(measures), 0.0001);
        final double[] view = cursor.viewOf(measures);
        assertEquals(4, view.length);
        assertEquals(300 * 0.75, view[0], 0.0001);
        assertEquals(400, view[1], 0.0001);
        assertEquals(500, view[2], 0.0001);
        assertEquals(600 * 0.4, view[3], 0.0001);
    }

    @Test
    void cursorOverOneMeasure() {
        final var timestamps = new long[] { 100, 200, 300, 400, 500, 600, 700, 800, 900 };

        final var cursor = Cursors.cursorOver(timestamps, 1, Duration.ofMillis(10), 0,
                100);

        assertEquals(0, cursor.startIndex());
        assertEquals(0, cursor.endIndex());
        assertEquals(0.1, cursor.firstMeasureRatio(), 0.0001);
        assertEquals(-1, cursor.lastMeasureRatio(), 0.0001);

        final var measures = new double[] { 100, 200, 300, 400, 500, 600, 700, 800, 900 };
        assertEquals(100 * 0.1, cursor.sum(measures), 0.0001);
        final double[] view = cursor.viewOf(measures);
        assertEquals(1, view.length);
        assertEquals(100 * 0.1, view[0], 0.0001);
    }
}
