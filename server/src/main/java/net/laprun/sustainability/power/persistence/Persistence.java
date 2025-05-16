package net.laprun.sustainability.power.persistence;

import jakarta.transaction.Transactional;

import net.laprun.sustainability.power.SensorMeasure;

public enum Persistence {
    ;

    @Transactional
    public static Measure save(SensorMeasure measure, String appName) {
        final var persisted = new Measure();
        persisted.components = measure.components();
        persisted.appName = appName;
        persisted.startTime = measure.startMs();
        persisted.endTime = measure.endMs();
        persisted.persist();
        return persisted;
    }
}
