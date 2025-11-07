package net.laprun.sustainability.power.sensors.linux.rapl;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import com.zaxxer.nuprocess.NuProcessBuilder;

import net.laprun.sustainability.power.nuprocess.BaseProcessHandler;

public class ProcessReadRAPLFile implements RAPLFile {
    private final NuProcessBuilder cat;
    private String read;

    public ProcessReadRAPLFile(Path path) {
        this(path, true);
    }

    ProcessReadRAPLFile(Path path, boolean sudo) {
        final var cmd = sudo ? new String[] { "sudo", "cat", path.toString() } : new String[] { "cat", path.toString() };
        final var handler = new BaseProcessHandler(cmd) {

            @Override
            public void onStdout(ByteBuffer buffer, boolean closed) {
                if (buffer.hasRemaining()) {
                    var bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    read = new String(bytes).trim();
                }
            }
        };
        cat = new NuProcessBuilder(handler, handler.command());
    }

    @Override
    public long extractEnergyInMicroJoules() {
        cat.run();
        return Long.parseLong(read);
    }

    @Override
    public String contentAsString() {
        cat.run();
        return read;
    }
}
