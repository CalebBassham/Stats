package me.calebbassham.stats;

import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.SQLException;

public class Stats {

    private static BasicDataSource dataSource;

    public static void setDataSource(BasicDataSource dataSource) {
        Stats.dataSource = dataSource;
    }

    public static BasicDataSource getDataSource() {
        return dataSource;
    }

    static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void setupDatabase() {
        var flyway = Flyway.configure()
                .table("stats_schema_history")
                .dataSource(dataSource).load();
        flyway.migrate();
    }


}
