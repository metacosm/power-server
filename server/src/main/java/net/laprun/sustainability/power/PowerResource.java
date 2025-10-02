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

@Path("/power")
public class PowerResource {
    @Inject
    PowerMeasurer measurer;

    public void onStartup(@Observes StartupEvent event) {
        Log.info("\nConfigured sampling period: " + samplingPeriod() +
                "\nDetected metadata:\n" + metadata());
    }

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

    @POST
    @Path("start/{appName}/{pid}")
    public void startMeasure(@PathParam("appName") String appName, @PathParam("pid") String pid) throws Exception {
        try {
            measurer.startTrackingApp(appName, measurer.validPIDOrFail(pid));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown process: " + pid);
        }
    }

    @GET
    @Path("metadata")
    public SensorMetadata metadata() {
        return measurer.metadata();
    }

    @GET
    @Path("sampling")
    public Duration samplingPeriod() {
        return measurer.getSamplingPeriod();
    }

    @GET
    @Path("measures/{appName}")
    public List<SensorMeasure> measures(@PathParam("appName") String appName) throws Exception {
        return Measure.forApplication(appName).stream().map(Measure::asSensorMeasure).toList();
    }
}
