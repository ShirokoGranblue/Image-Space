package db.migration.versioned;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class V3__AddPublicProfileStatsIndex extends BaseJavaMigration {

    private static final String TABLE = "images";
    private static final String INDEX_NAME = "idx_images_user_visibility_id";
    private static final List<String> INDEX_COLUMNS = List.of("user_id", "visibility", "id");

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (hasEquivalentIndex(connection)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `images` ADD INDEX `" + INDEX_NAME
                    + "` (`user_id`, `visibility`, `id`)");
        }
    }

    private boolean hasEquivalentIndex(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        Map<String, List<IndexColumn>> indexes = new LinkedHashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(
                connection.getCatalog(), null, TABLE, false, false)) {
            while (rows.next()) {
                String indexName = rows.getString("INDEX_NAME");
                String columnName = rows.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue;
                }
                indexes.computeIfAbsent(indexName, ignored -> new ArrayList<>())
                        .add(new IndexColumn(rows.getShort("ORDINAL_POSITION"), columnName));
            }
        }
        return indexes.values().stream()
                .map(columns -> columns.stream()
                        .sorted(Comparator.comparingInt(IndexColumn::position))
                        .map(IndexColumn::name)
                        .map(String::toLowerCase)
                        .toList())
                .anyMatch(INDEX_COLUMNS::equals);
    }

    private record IndexColumn(int position, String name) {}
}
