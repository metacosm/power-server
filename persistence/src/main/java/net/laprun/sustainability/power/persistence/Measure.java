package net.laprun.sustainability.power.persistence;

import java.util.List;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import net.laprun.sustainability.power.SensorMeasure;

@Entity
public class Measure extends PanacheEntity {
    public String appName;
    public long startTime;
    public long endTime;
    public double[] components;

    public static List<Measure> forApplication(String appName) {
        return find("appName", appName).list();
    }

    public static List<Measure> all() {
        return Measure.findAll().list();
    }

    public SensorMeasure asSensorMeasure() {
        return new SensorMeasure(components, startTime, endTime);
    }
}
