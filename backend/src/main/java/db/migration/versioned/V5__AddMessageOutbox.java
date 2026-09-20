package db.migration.versioned;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Statement;

public class V5__AddMessageOutbox extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS message_outbox (
                    event_id CHAR(36) PRIMARY KEY,
                    event_type VARCHAR(32) NOT NULL,
                    event_version INT NOT NULL DEFAULT 1,
                    payload LONGTEXT NOT NULL,
                    state VARCHAR(24) NOT NULL DEFAULT 'PENDING',
                    publish_token CHAR(36) NULL,
                    publish_attempts INT NOT NULL DEFAULT 0,
                    failures INT NOT NULL DEFAULT 0,
                    next_attempt_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    last_error VARCHAR(100) NULL,
                    INDEX idx_outbox_due (state, next_attempt_at),
                    INDEX idx_outbox_cleanup (state, updated_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            // Metadata checks also allow safe recovery after MySQL DDL auto-commits.
            try (var columns = context.getConnection().getMetaData().getColumns(
                    context.getConnection().getCatalog(), null, "audit_log", "event_id")) {
                if (!columns.next()) statement.execute("ALTER TABLE audit_log ADD COLUMN event_id CHAR(36) NULL");
            }
            boolean indexExists = false;
            try (var indexes = context.getConnection().getMetaData().getIndexInfo(
                    context.getConnection().getCatalog(), null, "audit_log", false, false)) {
                while (indexes.next()) {
                    if ("uk_audit_event".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) indexExists = true;
                }
            }
            if (!indexExists) statement.execute("CREATE UNIQUE INDEX uk_audit_event ON audit_log(event_id)");
        }
    }
}
