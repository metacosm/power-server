package net.laprun.sustainability.power.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

public class SQLiteFilePersister {
    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    @ConfigProperty(name = "power-server.db.location")
    String fileName;

    public void onStartup(@Observes StartupEvent event) {

    }

    public void onShutdown(@Observes ShutdownEvent event) {
        Log.info("Saving data to " + fileName);
        try (
                Connection connection = DriverManager.getConnection(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("backup to " + fileName);
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }
}
