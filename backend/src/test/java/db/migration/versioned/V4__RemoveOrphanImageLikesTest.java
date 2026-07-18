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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V4__RemoveOrphanImageLikesTest {

    @Test
    void migrateDeletesOnlyRowsWhoseImageNoLongerExists() throws Exception {
        List<String> statements = new ArrayList<>();

        new V4__RemoveOrphanImageLikes().migrate(
                context(Set.of("images", "image_likes"), statements)
        );

        assertEquals(List.of(V4__RemoveOrphanImageLikes.DELETE_ORPHANS_SQL), statements);
    }

    @Test
    void migrateSkipsCleanupWhenRequiredTableIsMissing() throws Exception {
        List<String> statements = new ArrayList<>();

        new V4__RemoveOrphanImageLikes().migrate(context(Set.of("images"), statements));

        assertEquals(0, statements.size());
    }

    private Context context(Set<String> tables, List<String> statements) throws Exception {
        Set<String> availableTables = new HashSet<>(tables);
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("picture_management");
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.createStatement()).thenReturn(statement);
        when(metadata.getTables(anyString(), isNull(), anyString(), any(String[].class)))
                .thenAnswer(invocation -> tableResult(
                        availableTables.contains(invocation.getArgument(2, String.class))
                ));
        when(statement.executeUpdate(anyString())).thenAnswer(invocation -> {
            statements.add(invocation.getArgument(0));
            return 0;
        });
        return context;
    }

    private ResultSet tableResult(boolean exists) throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(exists, false);
        return result;
    }
}
