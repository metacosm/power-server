package net.laprun.sustainability.power;

import java.time.Duration;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;

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
    @Path("{pid}")
    public Multi<SensorMeasure> powerFor(@PathParam("pid") String pid) throws Exception {
        try {
            return measurer.startTracking(pid);
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
}
