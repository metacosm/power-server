package net.laprun.sustainability.power.persistence;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import net.laprun.sustainability.power.SensorMeasure;

@Entity
public class Measure extends PanacheEntity {
    public String appName;
    public long startTime;
    public long endTime;
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
        return new SensorMeasure(components, startTime, endTime);
    }
}
