package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;

public class PowermetricsProcessHandler extends NuAbstractProcessHandler {
    private String errorMsg;
    private NuProcess process;
    private final String[] command;
    private ByteArrayInputStream bais;

    public PowermetricsProcessHandler(String... command) {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("No powermetrics options specified");
        }
        final var additionalArgsCardinality = 3;
        final var args = new String[command.length + additionalArgsCardinality];
        args[0] = "sudo";
        args[1] = "powermetrics";
        args[2] = "--samplers";
        System.arraycopy(command, 0, args, additionalArgsCardinality, command.length);
        this.command = args;
    }

    public String[] comand() {
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
        if (!closed) {
            bais = new ByteArrayInputStream(buffer.array());
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

    public InputStream getInputStream() {
        return bais;
    }

    public boolean isRunning() {
        return process != null && process.isRunning();
    }
}
