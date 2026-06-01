package net.laprun.sustainability.power;

import io.quarkus.test.Mock;
import net.laprun.sustainability.power.persistence.Measure;
import net.laprun.sustainability.power.persistence.Persistence;

@Mock
public class MockPersistence extends Persistence {

    public static final long ID = 1234L;

    @Override
    public Measure save(SensorMeasure measure, String appName, String session) {
        final var m = new Measure();
        m.id = ID;
        return m;
    }
}
