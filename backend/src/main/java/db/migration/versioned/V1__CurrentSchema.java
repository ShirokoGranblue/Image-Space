package db.migration.versioned;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class V1__CurrentSchema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) {
        ScriptUtils.executeSqlScript(
                context.getConnection(),
                new ClassPathResource("db/schema.sql")
        );
    }
}
