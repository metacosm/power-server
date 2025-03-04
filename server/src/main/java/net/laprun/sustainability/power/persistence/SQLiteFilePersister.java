package net.laprun.sustainability.power.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.internal.SessionImpl;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class SQLiteFilePersister {
    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    private static final Logger LOGGER = Logger.getLogger(SQLiteFilePersister.class.getName());

    @Inject
    EntityManager entityManager;

    private final AtomicBoolean executing = new AtomicBoolean(false);

    // Execute a backup every 10 seconds
    @Scheduled(delay = 10, delayUnit = TimeUnit.SECONDS, every = "10s")
    void scheduled() {
        backup();
    }

    // Execute a backup during shutdown
    public void onShutdown(@Observes ShutdownEvent event) {
        backup();
    }

    void backup() {
        if (executing.compareAndSet(false, true)) {
            try {
                int prefixLength = "jdbc:sqlite:".length();
                int queryParamsIdx = jdbcUrl.indexOf('?');
                int length = (queryParamsIdx != -1) ? queryParamsIdx : jdbcUrl.length();
                String dbFile = jdbcUrl.substring(prefixLength, length);

                var originalDbFilePath = Paths.get(dbFile);
                LOGGER.info("Starting DB backup for file: " + dbFile);
                var backupDbFilePath = originalDbFilePath.toAbsolutePath().getParent()
                        .resolve(originalDbFilePath.getFileName() + "_backup");

                JdbcConnectionAccess access = entityManager
                        .unwrap(SessionImpl.class)
                        .getJdbcConnectionAccess();
                try (var conn = access.obtainConnection();
                        var stmt = conn.createStatement()) {
                    // Execute the backup
                    stmt.executeUpdate("backup to " + backupDbFilePath);
                    // Atomically substitute the DB file with its backup
                    Files.move(backupDbFilePath, originalDbFilePath, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to backup the database", e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create backup files or folders", e);
                }
                LOGGER.info("Backup of " + dbFile + " completed.");
            } finally {
                executing.set(false);
            }
        } else {
            LOGGER.info("Backup in progress.");
        }
    }

}
