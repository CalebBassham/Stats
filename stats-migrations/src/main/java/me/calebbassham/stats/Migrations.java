package me.calebbassham.stats;

import org.flywaydb.core.Flyway;

import java.util.Scanner;

public class Migrations {

    public static void main(String[] args) {
        new Migrations().run();
    }

    public void run() {
        final var scanner = new Scanner(System.in);
        final var host = getHost(scanner);
        final var database = getDatabase(scanner);
        final var username = getUsername(scanner);
        final var password = getPassword(scanner);

        final var flyway = Flyway.configure()
                .table("stats_schema_history")
                .dataSource(String.format("jdbc:mysql://%s/%s", host, database), username, password)
                .load();

        flyway.migrate();
    }

    public String getHost(Scanner scanner) {
        System.out.print("Host: ");
        return scanner.nextLine();
    }

    public String getDatabase(Scanner scanner) {
        System.out.print("Database: ");
        return scanner.nextLine();
    }

    public String getUsername(Scanner scanner) {
        System.out.print("Username: ");
        return scanner.nextLine();
    }

    public String getPassword(Scanner scanner) {
        System.out.print("Password: ");
        return scanner.nextLine();
    }

}
