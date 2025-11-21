package net.laprun.sustainability.power.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.measures.NoDurationSensorMeasure;
import net.laprun.sustainability.power.measures.PartialSensorMeasure;

@Entity
public class Measure extends PanacheEntity {
    public String appName;
    public long startTime;
    public long endTime;
    public long duration;
    public double[] components;
    public String session;

    public static List<Measure> forApplication(String appName) {
        return find("appName", appName).list();
    }

    public static Stream<Measure> forApplicationSession(String appName, String session) {
        return find("appName = ?1 and session = ?2", appName, session).stream();
    }

    public static Stream<Measure> all() {
        return Measure.findAll().stream();
    }

    public SensorMeasure asSensorMeasure() {
        return isPartial() ? new PartialSensorMeasure(components, startTime, endTime, duration)
                : new NoDurationSensorMeasure(components, startTime, endTime);
    }

    public long duration() {
        return isPartial() ? duration : endTime - startTime;
    }

    public boolean isPartial() {
        return duration > 0;
    }

    @Override
    public String toString() {
        return super.toString() +
                "{appName='" + appName + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", components=" + Arrays.toString(components) +
                ", session='" + session + '\'' +
                '}';
    }
}
