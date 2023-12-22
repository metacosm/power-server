package io.github.metacosm.power.sensors.linux.rapl;

import io.github.metacosm.power.SensorMeasure;
import io.github.metacosm.power.SensorMetadata;
import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class IntelRAPLSensor implements PowerSensor {
    private final RAPLFile[] raplFiles;
    private final SensorMetadata metadata;
    private final double[] lastMeasuredSensorValues;
    private long frequency;
    private final SingleMeasureMeasures measures = new SingleMeasureMeasures();

    public IntelRAPLSensor() {
        // if we total system energy is not available, read package and DRAM if possible
        // todo: check Intel doc
        final var files = new TreeMap<String, RAPLFile>();
        if (!addFileIfReadable("/sys/class/powercap/intel-rapl/intel-rapl:1/energy_uj", files)) {
            addFileIfReadable("/sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj", files);
            addFileIfReadable("/sys/class/powercap/intel-rapl/intel-rapl:0/intel-rapl:0:2/energy_uj", files);
        }

        if (files.isEmpty())
            throw new RuntimeException("Failed to get RAPL energy readings, probably due to lack of read access ");

        raplFiles = files.values().toArray(new RAPLFile[0]);
        final var metadata = new HashMap<String, SensorMetadata.ComponentMetadata>(files.size());
        int fileNb = 0;
        for (String name : files.keySet()) {
            metadata.put(name, new SensorMetadata.ComponentMetadata(name, fileNb++, name, false, "µJ"));
        }
        this.metadata = new SensorMetadata(metadata, "Linux RAPL derived information, see https://www.kernel.org/doc/html/latest/power/powercap/powercap.html");
        lastMeasuredSensorValues = new double[raplFiles.length];
    }

    private boolean addFileIfReadable(String raplFileAsString, SortedMap<String, RAPLFile> files) {
        final var raplFile = Path.of(raplFileAsString);
        if (isReadable(raplFile)) {
            // get metric name
            final var nameFile = raplFile.resolveSibling("name");
            if (!isReadable(nameFile)) {
                throw new IllegalStateException("No name associated with " + raplFileAsString);
            }

            try {
                final var name = Files.readString(nameFile).trim();
                files.put(name, RAPLFile.createFrom(raplFile));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isReadable(Path file) {
        return Files.exists(file) && Files.isReadable(file);
    }


    @Override
    public void start(long frequency) throws Exception {
        this.frequency = frequency;

        // perform an initial measure to prime the data
        update(0L);
    }

    private double computeNewComponentValue(int componentIndex, long sensorValue) {
        return (sensorValue - lastMeasuredSensorValues[componentIndex]) / frequency / 1000;
    }

    @Override
    public SensorMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean isStarted() {
        return frequency > 0;
    }

    @Override
    public RegisteredPID register(long pid) {
        return measures.register(pid);
    }

    @Override
    public Measures update(Long tick) {
        final var measure = new double[raplFiles.length];
        for (int i = 0; i < raplFiles.length; i++) {
            final var value = raplFiles[i].extractPowerMeasure();
            final var newComponentValue = computeNewComponentValue(i, value);
            measure[i] = newComponentValue;
            lastMeasuredSensorValues[i] = newComponentValue;
        }
        measures.singleMeasure(new SensorMeasure(measure, tick));
        return measures;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.unregister(registeredPID);
    }
}
