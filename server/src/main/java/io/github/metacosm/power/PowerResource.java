package io.github.metacosm.power;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/power")
public class PowerResource {
    @Inject
    PowerMeasurer measurer;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("{pid}")
    public Multi<String> powerFor(@PathParam("pid") String pid) throws Exception {
        try {
            return measurer.startTracking(pid).map(array -> {
                String result = "\n";
                for (double v : array) {
                    result += v + " ";
                }
                return result;
            });
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
