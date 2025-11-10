package net.laprun.sustainability.power.sensors.linux.rapl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class RAPLFileTest {
    @Test
    void periodicReadingShouldWork() throws IOException, InterruptedException {
        final var file = Path.of("target/test.txt");
        final var raplFile = new ProcessReadRAPLFile(file, false);
        for (int i = 0; i < 5; i++) {
            writeThenRead(raplFile, file);
        }
    }

    private static void writeThenRead(RAPLFile raplFile, Path file) throws IOException, InterruptedException {
        final var value = Math.abs(new Random().nextLong());
        Files.writeString(file, value + "\n");
        Thread.sleep(25);

        final var measure = raplFile.extractEnergyInMicroJoules();
        assertEquals(value, measure);

        Files.writeString(file, "foo\n");
        Thread.sleep(25);
        assertEquals("foo", raplFile.contentAsString());
    }
}
