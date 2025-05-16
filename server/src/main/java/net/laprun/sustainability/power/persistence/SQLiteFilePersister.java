package net.laprun.sustainability.power.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.internal.SessionImpl;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.scheduler.Scheduled;

@Singleton
public class SQLiteFilePersister {
    private final AtomicBoolean executing = new AtomicBoolean(false);
    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;
    @Inject
    EntityManager entityManager;
    private Path dbFile;
    private Path backupDBFile;
    private JdbcConnectionAccess access;

    @PostConstruct
    void init() {
        int prefixLength = "jdbc:sqlite:".length();
        int queryParamsIdx = jdbcUrl.indexOf('?');
        int length = (queryParamsIdx != -1) ? queryParamsIdx : jdbcUrl.length();
        var dbFileName = jdbcUrl.substring(prefixLength, length);
        dbFile = Paths.get(dbFileName);
        backupDBFile = dbFile.toAbsolutePath().getParent().resolve(dbFile.getFileName() + "_backup");
        access = entityManager.unwrap(SessionImpl.class).getJdbcConnectionAccess();
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
                Log.trace("Starting DB backup for file: " + dbFile);
                try (var conn = access.obtainConnection();
                        var stmt = conn.createStatement()) {
                    // Execute the backup
                    stmt.executeUpdate("backup to " + backupDBFile);
                    // Atomically substitute the DB file with its backup
                    Files.move(backupDBFile, dbFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to backup the database", e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create backup files or folders", e);
                }
                Log.info("Backup of " + dbFile + " completed");
            } finally {
                executing.set(false);
            }
        } else {
            Log.trace("Skipping backup as one is already in progress");
        }
    }

}
