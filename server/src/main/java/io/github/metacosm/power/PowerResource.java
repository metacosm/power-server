package io.github.metacosm.power;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/power")
public class PowerResource {
    @Inject
    PowerMeasurer measurer;

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
}
