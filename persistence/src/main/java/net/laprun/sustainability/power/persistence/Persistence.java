package net.laprun.sustainability.power.persistence;

import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import net.laprun.sustainability.power.SensorMeasure;

@ApplicationScoped
public class Persistence {

    @Transactional
    public Measure save(SensorMeasure measure, String appName, String session) {
        final var persisted = new Measure();
        persisted.components = measure.components();
        persisted.appName = appName;
        persisted.session = session;
        persisted.startTime = measure.startMs();
        persisted.endTime = measure.endMs();
        persisted.persist();
        return persisted;
    }

    @Transactional
    public Optional<Double> synthesizeAndAggregateForSession(String appName, String session,
            Function<Measure, Double> synthesizer) {
        return Measure.forApplicationSession(appName, session)
                .filter(measure -> measure.components.length != 1)
                .map(synthesizer)
                .reduce(Double::sum);
    }
}
