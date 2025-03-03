package net.laprun.sustainability.power;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.prefs.Preferences;

import jakarta.inject.Singleton;

import io.smallrye.config.SmallRyeConfig;

@Singleton
public class Security {
    private final static boolean isRoot;
    public static final String SECRET_PROPERTY_KEY = "power-server.sudo.secret";

    static {
        isRoot = isRunningAsAdministrator();
    }

    private final String pwd;

    public Security(SmallRyeConfig config) {
        pwd = config.getConfigValue(SECRET_PROPERTY_KEY).getValue();
        if (pwd == null && !isRoot) {
            throw new IllegalStateException(
                    "This application requires sudo access. Either provide a sudo secret using the 'power-server.sudo.secret' property or run using sudo.");
        }
    }

    // figure out if we're running as admin by trying to write a system-level preference
    // see: https://stackoverflow.com/a/23538961/5752008
    private synchronized static boolean isRunningAsAdministrator() {
        final var preferences = Preferences.systemRoot();

        // avoid outputting errors
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        }));

        try {
            preferences.put("foo", "bar"); // SecurityException on Windows
            preferences.remove("foo");
            preferences.flush(); // BackingStoreException on Linux and macOS
            return true;
        } catch (Exception exception) {
            return false;
        } finally {
            System.setErr(System.err);
        }

    }

    public Process execPowermetrics(String... options) throws IOException {
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("No powermetrics options specified");
        }
        final var args = new String[options.length + 2];
        args[0] = "powermetrics";
        args[1] = "--samplers";
        System.arraycopy(options, 0, args, 2, options.length);
        return sudo(args);
    }

    public Process sudo(String... cmd) throws IOException {
        if (cmd == null || cmd.length == 0) {
            throw new IllegalArgumentException("No command specified to run with sudo");
        }

        if (!isRoot) {
            final var args = new String[cmd.length + 2];
            args[0] = "sudo";
            args[1] = "-S";
            System.arraycopy(cmd, 0, args, 2, cmd.length);
            final var runWithSudo = new ProcessBuilder(args);
            return ProcessBuilder.startPipeline(List.of(new ProcessBuilder("echo", pwd), runWithSudo)).getLast();
        } else {
            return new ProcessBuilder().command(cmd).start();
        }
    }
}
