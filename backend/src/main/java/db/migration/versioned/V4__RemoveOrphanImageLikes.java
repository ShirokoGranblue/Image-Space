package db.migration.versioned;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class V4__RemoveOrphanImageLikes extends BaseJavaMigration {

    static final String DELETE_ORPHANS_SQL = """
            DELETE il
            FROM image_likes il
            LEFT JOIN images i ON i.id = il.image_id
            WHERE i.id IS NULL
            """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "images") || !tableExists(connection, "image_likes")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(DELETE_ORPHANS_SQL);
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
