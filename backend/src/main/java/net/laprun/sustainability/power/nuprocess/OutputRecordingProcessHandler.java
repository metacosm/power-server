package net.laprun.sustainability.power.nuprocess;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class OutputRecordingProcessHandler extends BaseProcessHandler {
    private final GrowableBuffer stdOutBuffer = new GrowableBuffer();
    private final CompletableFuture<InputStream> output = new CompletableFuture<>();

    public OutputRecordingProcessHandler(String... command) {
        super(command);
    }

    @Override
    public void onStdout(ByteBuffer buffer, boolean closed) {
        if (buffer.hasRemaining()) {
            stdOutBuffer.put(buffer);
        }

        if (closed) {
            output.complete(new ByteArrayInputStream(stdOutBuffer.array()));
        }
    }

    public Future<InputStream> getInputStream() {
        return output;
    }
}
