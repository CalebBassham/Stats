package me.calebbassham.stats;

import java.sql.Connection;
import java.util.function.Supplier;

public class Stats {

    private static Supplier<Connection> connectionSupplier;

    public static void setConnectionSupplier(Supplier<Connection> connectionProvider) {
        connectionSupplier = connectionProvider;
    }

    static Connection getConnection() {
        return connectionSupplier.get();
    }

}
