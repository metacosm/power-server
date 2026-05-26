package net.laprun.sustainability.power.nuprocess;

import java.io.InputStream;
import java.nio.ByteBuffer;

import com.zaxxer.nuprocess.NuProcess;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.subscription.MultiEmitter;
import net.laprun.sustainability.power.sensors.macos.powermetrics.ProcessWrapper;

public class EmitterProcessHandler extends BaseProcessHandler {
    private final boolean debug = true;
    private MultiEmitter<? super InputStream> emitter;
    private long start;
    private short ignoreCount = 2;

    public EmitterProcessHandler(long periodInMilliSecondsAsString) {
        super(ProcessWrapper.preparePowermetricsCommandVarArgs("cpu_power,tasks",
                "--show-process-samp-norm", "-b", "65536", "--show-process-gpu", "-i",
                "" + periodInMilliSecondsAsString));
    }

    public void setEmitter(MultiEmitter<? super InputStream> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onPreStart(NuProcess nuProcess) {
        start = System.currentTimeMillis();
        super.onPreStart(nuProcess);
    }

    @Override
    public void onStdout(ByteBuffer buffer, boolean closed) {
        if (ignoreCount-- > 0) {
            return;
        }
        final var now = System.currentTimeMillis();
        if (buffer.hasRemaining() && !closed) {
            final var remaining = buffer.remaining();

            final var readOnlyBuffer = buffer.asReadOnlyBuffer();
            final var first3Chars = new byte[4];
            readOnlyBuffer.get(first3Chars);
            Log.infof("1st 4 bytes: '%s'", new String(first3Chars));
            final var last3Chars = new byte[4];
            readOnlyBuffer.get(remaining - 4, last3Chars, 0, 3);
            Log.infof("last 4 bytes: '%s'", new String(last3Chars));

            if (debug) {
                var bytes = new byte[remaining];
                buffer.duplicate().get(bytes);
                Log.infof("=== read %d after %d:\n\n'%s'\n\n===\n", remaining, now - start,
                        new String(bytes));
                start = now;
            }
            final var shouldBuffer = remaining > 16384;
            if (!shouldBuffer) {
                return;
            }
            //            log("read", remaining);
            //            stdOutBuffer.put(buffer);
            //                Log.warnf("=== read %s", new String(stdOutBuffer.array()));
            //emitter.emit(new ByteArrayInputStream(bytes));
            //            stdOutBuffer.clear();
        }

        if (closed) {
            log("closed", 0);
            //emitter.complete();
        }
    }

    private void log(String op, int remaining) {
        final var end = System.currentTimeMillis();
        Log.infof("%s after %dms, size: %d", op, end - start, remaining);
        start = end;
    }
}
