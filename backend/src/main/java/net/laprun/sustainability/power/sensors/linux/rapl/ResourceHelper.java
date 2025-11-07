package net.laprun.sustainability.power.sensors.linux.rapl;

import java.io.File;
import java.nio.file.Path;

enum ResourceHelper {
    ;
    static Path getResourcePath(Class<?> clazz, String resourceName) {
        return Path.of(getResourcePathAsString(clazz, resourceName));
    }

    static String getResourcePathAsString(Class<?> clazz, String resourceName) {
        return new File(clazz.getClassLoader().getResource(resourceName).getFile()).getAbsolutePath();
    }
}
