package net.laprun.sustainability.power.nuprocess;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.quarkus.logging.Log;

public class OutputRecordingProcessHandler extends BaseProcessHandler {
    private final GrowableBuffer stdOutBuffer;
    private final CompletableFuture<InputStream> output = new CompletableFuture<>();
    private final boolean debug = false;

    public OutputRecordingProcessHandler(int bufferSize, String... command) {
        super(command);
        stdOutBuffer = new GrowableBuffer(bufferSize);
    }

    @Override
    public void onStdout(ByteBuffer buffer, boolean closed) {
        if (buffer.hasRemaining() && !closed) {
            if (debug) {
                final var remaining = buffer.remaining();
                var bytes = new byte[remaining];
                buffer.duplicate().get(bytes);
                final var read = new String(bytes);
                Log.infof("=== read %d:\n\n%s\n\n===\n", remaining, read);
            }
            stdOutBuffer.put(buffer);
        }

        if (closed) {
            final var array = stdOutBuffer.array();
            if (debug) {
                Log.infof("array %d:\n\n%s\n\n", array.length, new String(array));
            }
            output.complete(new ByteArrayInputStream(array));
        }
    }

    public Future<InputStream> getInputStream() {
        return output;
    }
}
