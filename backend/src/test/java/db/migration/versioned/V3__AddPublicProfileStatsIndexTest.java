package db.migration.versioned;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V3__AddPublicProfileStatsIndexTest {

    @Test
    void migrateAddsEquivalentIndexOnlyOnce() throws Exception {
        List<IndexColumn> indexes = new ArrayList<>();
        indexes.add(new IndexColumn("idx_user_id", "user_id", 1));
        List<String> statements = new ArrayList<>();
        Context context = context(indexes, statements);
        V3__AddPublicProfileStatsIndex migration = new V3__AddPublicProfileStatsIndex();

        migration.migrate(context);
        migration.migrate(context);

        assertEquals(1, statements.size());
        assertEquals(
                "ALTER TABLE `images` ADD INDEX `idx_images_user_visibility_id` (`user_id`, `visibility`, `id`)",
                statements.get(0)
        );
    }

    @Test
    void migrateAcceptsEquivalentIndexWithDifferentName() throws Exception {
        List<IndexColumn> indexes = new ArrayList<>(List.of(
                new IndexColumn("idx_existing", "user_id", 1),
                new IndexColumn("idx_existing", "visibility", 2),
                new IndexColumn("idx_existing", "id", 3)
        ));
        List<String> statements = new ArrayList<>();

        new V3__AddPublicProfileStatsIndex().migrate(context(indexes, statements));

        assertEquals(0, statements.size());
    }

    private Context context(List<IndexColumn> indexes, List<String> statements) throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("picture_management");
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.createStatement()).thenReturn(statement);
        when(metadata.getIndexInfo(anyString(), isNull(), anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(ignored -> indexRows(indexes));
        when(statement.execute(anyString())).thenAnswer(invocation -> {
            statements.add(invocation.getArgument(0));
            indexes.add(new IndexColumn("idx_images_user_visibility_id", "user_id", 1));
            indexes.add(new IndexColumn("idx_images_user_visibility_id", "visibility", 2));
            indexes.add(new IndexColumn("idx_images_user_visibility_id", "id", 3));
            return true;
        });
        return context;
    }

    private ResultSet indexRows(List<IndexColumn> indexes) throws Exception {
        ResultSet result = mock(ResultSet.class);
        int[] position = {-1};
        when(result.next()).thenAnswer(ignored -> ++position[0] < indexes.size());
        when(result.getString("INDEX_NAME")).thenAnswer(ignored -> indexes.get(position[0]).indexName());
        when(result.getString("COLUMN_NAME")).thenAnswer(ignored -> indexes.get(position[0]).columnName());
        when(result.getShort("ORDINAL_POSITION"))
                .thenAnswer(ignored -> (short) indexes.get(position[0]).position());
        return result;
    }

    private record IndexColumn(String indexName, String columnName, int position) {}
}
