package net.laprun.sustainability.power.sensors.linux.rapl;

import static net.laprun.sustainability.power.SensorUnit.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.AbstractPowerSensor;
import net.laprun.sustainability.power.sensors.Measures;

/**
 * A sensor using Intel's RAPL accessed via Linux' powercap system.
 */
public class IntelRAPLSensor extends AbstractPowerSensor<SingleMeasureMeasures> {
    private final RAPLFile[] raplFiles;
    private final SensorMetadata metadata;
    private final double[] lastMeasuredSensorValues;
    private long frequency;

    /**
     * Initializes the RAPL sensor
     */
    public IntelRAPLSensor() {
        this(defaultRAPLFiles());
    }

    protected IntelRAPLSensor(String... raplFilePaths) {
        this(fromPaths(raplFilePaths));
    }

    private static SortedMap<String, RAPLFile> fromPaths(String... raplFilePaths) {
        if (raplFilePaths == null || raplFilePaths.length == 0) {
            throw new IllegalArgumentException("Must provide at least one RAPL file");
        }

        final var files = new TreeMap<String, RAPLFile>();
        for (String raplFilePath : raplFilePaths) {
            addFileIfReadable(raplFilePath, files);
        }
        return files;
    }

    private static SortedMap<String, RAPLFile> defaultRAPLFiles() {
        // if we total system energy is not available, read package and DRAM if possible
        // todo: check Intel doc
        final var files = new TreeMap<String, RAPLFile>();
        if (!addFileIfReadable("/sys/class/powercap/intel-rapl/intel-rapl:1/energy_uj", files)) {
            addFileIfReadable("/sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj", files);
            addFileIfReadable("/sys/class/powercap/intel-rapl/intel-rapl:0/intel-rapl:0:2/energy_uj", files);
        }
        return files;
    }

    private IntelRAPLSensor(SortedMap<String, RAPLFile> files) {
        super(new SingleMeasureMeasures());
        if (files.isEmpty())
            throw new RuntimeException("Failed to get RAPL energy readings, probably due to lack of read access ");

        raplFiles = files.values().toArray(new RAPLFile[0]);
        final var rawOffset = files.size();
        final var metadata = new ArrayList<SensorMetadata.ComponentMetadata>(rawOffset * 2);
        int fileNb = 0;
        for (String name : files.keySet()) {
            metadata.add(new SensorMetadata.ComponentMetadata(name, fileNb, name, false, mW));
            final var rawName = name + "_uj";
            metadata.add(new SensorMetadata.ComponentMetadata(rawName, fileNb + rawOffset,
                    name + " (raw micro Joule data)", false, µJ));
            fileNb++;
        }
        this.metadata = new SensorMetadata(metadata,
                "Linux RAPL derived information, see https://www.kernel.org/doc/html/latest/power/powercap/powercap.html");
        lastMeasuredSensorValues = new double[raplFiles.length];
    }

    private static boolean addFileIfReadable(String raplFileAsString, SortedMap<String, RAPLFile> files) {
        final var path = Path.of(raplFileAsString);
        if (Files.exists(path)) {
            // get metric name
            final var nameFile = path.resolveSibling("name");
            if (!Files.exists(nameFile)) {
                throw new IllegalStateException("No name associated with " + raplFileAsString);
            }

            try {
                final var raplFile = RAPLFile.createFrom(path);
                final var name = RAPLFile.createFrom(nameFile).contentAsString().trim();
                files.put(name, raplFile);
            } catch (Exception e) {
                Log.debugf("Couldn't read file: %s, cause: %s", nameFile, e);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void doStart(long frequency) {
        this.frequency = frequency;

        // perform an initial measure to prime the data
        update(0L);
    }

    /**
     * Computes the power in mW based on the current, previous energy (in micro Joules) measures and sampling frequency.
     *
     * @param componentIndex the index of the component being measured
     * @param sensorValue the micro Joules energy reading
     * @param newMeasureTime the epoch of the new measure being taken
     * @return the power over the interval defined by the sampling frequency in mW
     */
    private double computePowerInMilliWatt(int componentIndex, long sensorValue, long newMeasureTime) {
        return (sensorValue - lastMeasuredSensorValues[componentIndex]) / (newMeasureTime - lastUpdateEpoch()) / 1000;
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
    protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch) {
        final var measure = new double[raplFiles.length];
        for (int i = 0; i < raplFiles.length; i++) {
            final var value = raplFiles[i].extractEnergyInMicroJoules();
            final var newComponentValue = computePowerInMilliWatt(i, value, newUpdateStartEpoch);
            measure[i] = newComponentValue;
            lastMeasuredSensorValues[i] = newComponentValue;
        }
        measures.singleMeasure(new SensorMeasure(measure, lastUpdateEpoch, newUpdateStartEpoch));
        return measures;
    }
}
