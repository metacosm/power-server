package net.laprun.sustainability.power.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import net.laprun.sustainability.power.SensorMeasure;

@ApplicationScoped
public class Persistence {

    @Transactional
    public Measure save(SensorMeasure measure, String appName) {
        final var persisted = new Measure();
        persisted.components = measure.components();
        persisted.appName = appName;
        persisted.startTime = measure.startMs();
        persisted.endTime = measure.endMs();
        persisted.persist();
        return persisted;
    }
}
