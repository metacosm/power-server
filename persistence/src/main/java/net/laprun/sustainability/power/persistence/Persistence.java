package net.laprun.sustainability.power.persistence;

import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.SensorMeasure;

@ApplicationScoped
public class Persistence {
    public static final String SYSTEM_TOTAL_APP_NAME = "system:total";

    @Transactional
    public Measure save(SensorMeasure measure, String appName, String session) {
        if (SensorMeasure.missing == measure) {
            Log.debugf("Ignoring missing measure for app: %s, session: %s", appName, session);
            return null;
        }
        final var persisted = new Measure();
        persisted.components = measure.components();
        persisted.appName = appName;
        persisted.session = session == null ? defaultSession(appName) : session;
        persisted.startTime = measure.startMs();
        persisted.endTime = measure.endMs();
        persisted.duration = measure.durationMs();
        persisted.externalCPUShare = measure.externalCPUShare();
        persisted.persist();
        Log.debugf("Persisted %s, measure duration: %sms", persisted, persisted.endTime - persisted.startTime);
        return persisted;
    }

    @Transactional
    public Measure save(SensorMeasure measure, String appName) {
        return save(measure, appName, null);
    }

    @Transactional
    public Optional<Double> synthesizeAndAggregateForSession(String appName, String session,
            Function<Measure, Double> synthesizer) {
        return Measure.forApplicationSession(appName, session)
                .map(synthesizer)
                .reduce(Double::sum);
    }

    public static String defaultSession(String appName) {
        return appName + "-" + System.currentTimeMillis();
    }
}
