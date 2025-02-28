package net.laprun.sustainability.power.persistence;

import jakarta.transaction.Transactional;

import net.laprun.sustainability.power.SensorMeasure;

public enum Persistence {
    ;

    @Transactional
    public static Measure save(SensorMeasure measure, long parsedPID) {
        final var persisted = new Measure();
        persisted.components = measure.components();
        persisted.pid = parsedPID;
        persisted.startTime = measure.timestamp();
        persisted.endTime = measure.timestamp() + measure.duration();
        persisted.persist();
        return persisted;
    }
}
