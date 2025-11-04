package net.laprun.sustainability.power;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import net.laprun.sustainability.power.persistence.Measure;
import net.laprun.sustainability.power.persistence.Persistence;
import net.laprun.sustainability.power.sensors.SamplingMeasurer;

/**
 * A RESTful endpoint to measure the power consumption associated with processes running on the current system
 */
@Path("/power")
public class PowerResource {
    @Inject
    SamplingMeasurer measurer;

    public void onStartup(@Observes StartupEvent event) {
        Log.info("\nConfigured sampling period: " + samplingPeriod() +
                "\nDetected metadata:\n" + metadata());
    }

    /**
     * Streams power consumption measures associated with the specified process id, if such process is currently running on the
     * system.
     *
     * @param pid the process identified for which we want to stream measures
     * @return a stream of {@link SensorMeasure}
     * @throws Exception if an error occurred while measuring the power consumption
     */
    @GET
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Path("stream/{pid}")
    public Multi<SensorMeasure> streamMeasuresFor(@PathParam("pid") String pid) throws Exception {
        try {
            return measurer.stream(pid);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown process: " + pid);
        }
    }

    /**
     * Starts measuring the power consumption of the specified application as identified with the specified name and process id
     *
     * @param appName the application name, used to correlate several measures across different runs
     * @param pid the process id associated with the current run of the application
     * @throws Exception if an error occurred while measuring the power consumption
     */
    @POST
    @Path("start/{appName}/{pid}")
    public void startMeasure(@PathParam("appName") String appName, @PathParam("pid") String pid) throws Exception {
        try {
            measurer.startTrackingApp(appName, measurer.validPIDOrFail(pid), Persistence.defaultSession(appName));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown process: " + pid);
        }
    }

    /**
     * Retrieves the metadata associated with the power sensors of the underlying platform
     *
     * @return the {@link SensorMetadata} associated with the platform's sensors
     */
    @GET
    @Path("metadata")
    public SensorMetadata metadata() {
        return measurer.metadata();
    }

    /**
     * Retrieves the currently used sampling period
     *
     * @return the sampling period
     */
    @GET
    @Path("sampling")
    public Duration samplingPeriod() {
        return measurer.samplingPeriod();
    }

    /**
     * Retrieves all recorded measures associated with the specified application
     *
     * @param appName the application identifier as provided to {@link #startMeasure(String, String)}
     * @return the chronological list of measures associated with the specified application
     */
    @GET
    @Path("measures/{appName}")
    public List<SensorMeasure> measures(@PathParam("appName") String appName) {
        return Measure.forApplication(appName).stream().map(Measure::asSensorMeasure).toList();
    }
}
