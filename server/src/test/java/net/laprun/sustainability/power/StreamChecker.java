package net.laprun.sustainability.power;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.sse.SseEventSource;

import org.assertj.core.api.Assertions;

public class StreamChecker {

    static void checkPowerForPID(URI uri, long pid) throws Exception {
        final var powerForPid = ClientBuilder.newClient().target(uri.resolve("power"))
                .path("stream")
                .path("{pid}").resolveTemplate("pid", pid);

        try (final var eventSource = SseEventSource.target(powerForPid).build()) {
            CompletableFuture<List<String>> res = new CompletableFuture<>();
            List<String> collect = Collections.synchronizedList(new ArrayList<>());
            eventSource.register(inboundSseEvent -> {
                collect.add(inboundSseEvent.readData());
                // stop after one event
                eventSource.close();
            },
                    res::completeExceptionally,
                    () -> res.complete(collect));
            eventSource.open();
            Assertions.assertThat(res.get(5, TimeUnit.SECONDS)).hasSize(1);
        }
    }
}
