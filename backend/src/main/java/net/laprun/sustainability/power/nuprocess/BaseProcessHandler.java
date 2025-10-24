package net.laprun.sustainability.power.nuprocess;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;

public class BaseProcessHandler extends NuAbstractProcessHandler {
    private String errorMsg;
    private NuProcess process;
    private final String[] command;
    private final GrowableBuffer stdOutBuffer = new GrowableBuffer();
    private final CompletableFuture<InputStream> output = new CompletableFuture<>();

    public BaseProcessHandler(String... command) {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("No command specified");
        }
        this.command = initCommand(command);
    }

    protected String[] initCommand(String... command) {
        return command;
    }

    public String[] command() {
        return command;
    }

    @Override
    public void onPreStart(NuProcess nuProcess) {
        this.process = nuProcess;
    }

    @Override
    public void onExit(int statusCode) {
        if (Integer.MIN_VALUE == statusCode) {
            throw new IllegalArgumentException("Unknown command " + Arrays.toString(command));
        }
        if (statusCode != 0) {
            throw new RuntimeException("Couldn't execute command " + Arrays.toString(command)
                    + ". Error code: " + statusCode + ", message: " + errorMsg);
        }
    }

    public void stop() {
        if (process.isRunning()) {
            process.destroy(false);
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                process.destroy(true);
            }
        }
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

    @Override
    public void onStderr(ByteBuffer buffer, boolean closed) {
        if (!closed) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            errorMsg = new String(bytes);
        }
        super.onStderr(buffer, closed);
    }

    public Future<InputStream> getInputStream() {
        return output;
    }

    public boolean isRunning() {
        return process != null && process.isRunning();
    }
}
