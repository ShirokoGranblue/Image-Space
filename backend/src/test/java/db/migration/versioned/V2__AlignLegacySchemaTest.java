package db.migration.versioned;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V2__AlignLegacySchemaTest {

    private static final Pattern ADD_COLUMN = Pattern.compile(
            "ALTER TABLE `([^`]+)` ADD COLUMN `([^`]+)`"
    );
    private static final Pattern ADD_INDEX = Pattern.compile(
            "ALTER TABLE `([^`]+)` ADD (UNIQUE )?INDEX `[^`]+` [(]`([^`]+)`[)]"
    );

    @Test
    void migrateAddsMissingSchemaItemsOnlyOnce() throws Exception {
        Set<String> columns = new HashSet<>(List.of(
                "users.uuid", "users.github_username", "users.avatar_key", "users.background_key", "users.deleted",
                "users.email", "users.phone",
                "images.uuid", "images.storage_key", "images.original_key", "images.original_filename",
                "images.original_content_type", "images.original_ext", "images.original_size", "images.width",
                "images.height", "images.medium_key", "images.thumb_key", "images.media_version",
                "images.visible_usernames", "comments.image_key", "audit_log.response_result", "audit_log.status",
                "audit_log.risk_level", "audit_log.cost_time"
        ));
        Set<IndexState> indexes = new HashSet<>(Set.of(
                new IndexState("users", "uuid", true),
                new IndexState("users", "email", true),
                new IndexState("users", "phone", true),
                new IndexState("images", "uuid", true),
                new IndexState("audit_log", "status", false)
        ));
        List<String> executedSql = new ArrayList<>();

        Context context = context(columns, indexes, executedSql);
        V2__AlignLegacySchema migration = new V2__AlignLegacySchema();

        migration.migrate(context);
        migration.migrate(context);

        assertEquals(1, count(executedSql, "ADD COLUMN `email_verified`"));
        assertEquals(1, count(executedSql, "ADD INDEX `idx_risk_level`"));
    }

    private Context context(
            Set<String> columns,
            Set<IndexState> indexes,
            List<String> executedSql
    ) throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);

        when(context.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("picture_management");
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.createStatement()).thenReturn(statement);
        when(metadata.getColumns(anyString(), any(), anyString(), anyString())).thenAnswer(invocation -> {
            String table = invocation.getArgument(2);
            String column = invocation.getArgument(3);
            ResultSet result = mock(ResultSet.class);
            when(result.next()).thenAnswer(ignored -> columns.contains(table + "." + column));
            return result;
        });
        when(metadata.getIndexInfo(anyString(), any(), anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> indexResult(indexes, invocation.getArgument(2)));
        when(statement.execute(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            executedSql.add(sql);
            Matcher columnMatcher = ADD_COLUMN.matcher(sql);
            if (columnMatcher.find()) {
                columns.add(columnMatcher.group(1) + "." + columnMatcher.group(2));
            }
            Matcher indexMatcher = ADD_INDEX.matcher(sql);
            if (indexMatcher.find()) {
                indexes.add(new IndexState(
                        indexMatcher.group(1),
                        indexMatcher.group(3),
                        indexMatcher.group(2) != null
                ));
            }
            return true;
        });
        return context;
    }

    private ResultSet indexResult(Set<IndexState> indexes, String table) throws Exception {
        List<IndexState> rows = indexes.stream()
                .filter(index -> index.table().equals(table))
                .toList();
        ResultSet result = mock(ResultSet.class);
        int[] position = {-1};
        when(result.next()).thenAnswer(ignored -> ++position[0] < rows.size());
        when(result.getString("COLUMN_NAME")).thenAnswer(ignored -> rows.get(position[0]).column());
        when(result.getBoolean("NON_UNIQUE")).thenAnswer(ignored -> !rows.get(position[0]).unique());
        return result;
    }

    private long count(List<String> statements, String fragment) {
        return statements.stream().filter(statement -> statement.contains(fragment)).count();
    }

    private record IndexState(String table, String column, boolean unique) {}
}
