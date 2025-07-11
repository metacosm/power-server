package net.laprun.sustainability.power.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.scheduler.Scheduled;

@Singleton
public class SQLiteFilePersister {
    private final AtomicBoolean executing = new AtomicBoolean(false);
    @ConfigProperty(name = "power-server.db.backup.location")
    String dbFilelocation;
    @Inject
    DataSource dataSource;
    private Path dbFile;
    private Path backupDBFile;

    @PostConstruct
    void init() {
        dbFile = Paths.get(dbFilelocation);
        backupDBFile = dbFile.toAbsolutePath().getParent().resolve(dbFile.getFileName() + ".backup");
    }

    // Periodical backup
    @Scheduled(delay = 5, delayUnit = TimeUnit.MINUTES, every = "${power-server.db.backup.period}")
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

                Log.trace("Persisting database to: " + dbFile);
                try (var conn = dataSource.getConnection();
                        var stmt = conn.createStatement()) {
                    // Execute the backup
                    stmt.executeUpdate("backup to " + backupDBFile);
                    // Atomically substitute the DB file with its backup
                    Files.move(backupDBFile, dbFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to persist the database", e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create backup files or folders", e);
                }
                Log.info("Persisting " + dbFile + " completed");
            } finally {
                executing.set(false);
            }
        } else {
            Log.trace("Skipping database persistence as the operation is already in progress");
        }
    }

}
