package db.migration.versioned;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class V2__AlignLegacySchema extends BaseJavaMigration {

    private static final List<Column> COLUMNS = List.of(
            new Column("users", "uuid", "VARCHAR(36) NULL"),
            new Column("users", "email_verified", "TINYINT NOT NULL DEFAULT 0"),
            new Column("users", "github_username", "VARCHAR(100)"),
            new Column("users", "avatar_key", "VARCHAR(500)"),
            new Column("users", "background_key", "VARCHAR(500)"),
            new Column("users", "deleted", "TINYINT NOT NULL DEFAULT 0"),
            new Column("images", "uuid", "VARCHAR(36) NULL"),
            new Column("images", "storage_key", "VARCHAR(500)"),
            new Column("images", "original_key", "VARCHAR(500)"),
            new Column("images", "original_filename", "VARCHAR(255)"),
            new Column("images", "original_content_type", "VARCHAR(100)"),
            new Column("images", "original_ext", "VARCHAR(20)"),
            new Column("images", "original_size", "BIGINT"),
            new Column("images", "width", "INT"),
            new Column("images", "height", "INT"),
            new Column("images", "medium_key", "VARCHAR(500)"),
            new Column("images", "thumb_key", "VARCHAR(500)"),
            new Column("images", "media_version", "BIGINT NOT NULL DEFAULT 1"),
            new Column("images", "visible_usernames", "VARCHAR(500)"),
            new Column("comments", "image_key", "VARCHAR(500)"),
            new Column("audit_log", "response_result", "TEXT"),
            new Column("audit_log", "status", "VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'"),
            new Column("audit_log", "risk_level", "VARCHAR(20) NOT NULL DEFAULT 'LOW'"),
            new Column("audit_log", "cost_time", "BIGINT DEFAULT 0")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (Column column : COLUMNS) {
            addColumnIfMissing(connection, column);
        }

        normalizeNullableUniqueValues(connection, "email");
        normalizeNullableUniqueValues(connection, "phone");
        alignUuid(connection, "users", "uk_users_uuid");
        alignUuid(connection, "images", "uk_images_uuid");
        addUniqueIndexIfMissing(connection, "users", "email", "uk_users_email");
        addUniqueIndexIfMissing(connection, "users", "phone", "uk_users_phone");
        addIndexIfMissing(connection, "audit_log", "status", "idx_status");
        addIndexIfMissing(connection, "audit_log", "risk_level", "idx_risk_level");
    }

    private void addColumnIfMissing(Connection connection, Column column) throws SQLException {
        if (columnExists(connection, column.table(), column.name())) return;
        execute(connection, "ALTER TABLE `" + column.table() + "` ADD COLUMN `"
                + column.name() + "` " + column.definition());
    }

    private void normalizeNullableUniqueValues(Connection connection, String column) throws SQLException {
        if (!columnExists(connection, "users", column)) return;
        execute(connection, "UPDATE `users` SET `" + column + "` = NULL WHERE `" + column + "` = ''");
    }

    private void alignUuid(Connection connection, String table, String indexName) throws SQLException {
        execute(connection, "UPDATE `" + table + "` SET `uuid` = UUID() WHERE `uuid` IS NULL OR `uuid` = ''");
        execute(connection, "ALTER TABLE `" + table + "` MODIFY COLUMN `uuid` VARCHAR(36) NOT NULL");
        addUniqueIndexIfMissing(connection, table, "uuid", indexName);
    }

    private void addUniqueIndexIfMissing(
            Connection connection,
            String table,
            String column,
            String indexName
    ) throws SQLException {
        if (!columnExists(connection, table, column) || indexExists(connection, table, column, true)) return;
        execute(connection, "ALTER TABLE `" + table + "` ADD UNIQUE INDEX `"
                + indexName + "` (`" + column + "`)");
    }

    private void addIndexIfMissing(
            Connection connection,
            String table,
            String column,
            String indexName
    ) throws SQLException {
        if (!columnExists(connection, table, column) || indexExists(connection, table, column, false)) return;
        execute(connection, "ALTER TABLE `" + table + "` ADD INDEX `"
                + indexName + "` (`" + column + "`)");
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, table, column)) {
            return columns.next();
        }
    }

    private boolean indexExists(
            Connection connection,
            String table,
            String column,
            boolean unique
    ) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (indexes.next()) {
                String indexedColumn = indexes.getString("COLUMN_NAME");
                boolean nonUnique = indexes.getBoolean("NON_UNIQUE");
                if (column.equalsIgnoreCase(indexedColumn) && (!unique || !nonUnique)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record Column(String table, String name, String definition) {}
}
