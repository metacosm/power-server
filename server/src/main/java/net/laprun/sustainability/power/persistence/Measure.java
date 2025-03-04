package net.laprun.sustainability.power.persistence;

import java.util.List;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Measure extends PanacheEntity {
    public long pid;
    public long startTime;
    public long endTime;
    public double[] components;

    public static List<Measure> forPID(long pid) {
        return find("pid", pid).list();
    }

    public static List<Measure> all() {
        return Measure.findAll().list();
    }
}
