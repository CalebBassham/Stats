package me.calebbassham.stats.api;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class Game {

    private int id;
    private Instant startTime;
    private Instant endTime;

    private Game(int id, Instant startTime, Instant endTime) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Game create() throws SQLException {
        var conn = Stats.getConnection();

        var update = conn.prepareStatement("INSERT INTO game (start_time) VALUES (?)");
        update.setTimestamp(1, Timestamp.from(Instant.now()));
        update.executeUpdate();
        update.close();

        var query = conn.prepareStatement("SELECT * FROM game ORDER BY id DESC LIMIT 1");
        var rs = query.executeQuery();
        rs.next();
        var id = rs.getInt("id");
        var startTime = rs.getTimestamp("start_time").toInstant();
        rs.close();
        query.close();

        conn.close();

        return new Game(id, startTime, null);
    }

    public static Game load(int id) throws SQLException {
        var conn = Stats.getConnection();
        var query = conn.prepareStatement("SELECT * FROM game WHERE id = ?");
        query.setInt(1, id);
        var rs = query.executeQuery();
        rs.next();
        var startTime = rs.getTimestamp("start_time").toInstant();

        Instant endTime;
        try {
            endTime = rs.getTimestamp("end_time").toInstant();
        } catch (SQLException e) {
            if (e.getMessage().equals("Value '0000-00-00 00:00:00' can not be represented as java.sql.Timestamp")) {
                endTime = null;
            } else {
                throw e;
            }
        }

        rs.close();
        query.close();
        conn.close();
        return new Game(id, startTime, endTime);
    }

    public void delete() throws SQLException {
        var conn = Stats.getConnection();
        var query = conn.prepareStatement("DELETE FROM game WHERE id = ?");
        query.setInt(1, id);
        query.executeUpdate();

        query.close();
        conn.close();
    }

    public static void deleteLast() throws SQLException {
        var conn = Stats.getConnection();
        var query = conn.prepareStatement("DELETE FROM game ORDER BY id DESC LIMIT 1");
        query.executeUpdate();

        query.close();
        conn.close();
    }

    public int getId() {
        return id;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setEndTime(Instant endTime) throws SQLException {
        var conn = Stats.getConnection();
        var stmt = conn.prepareStatement("UPDATE game SET end_time = ? WHERE id = ?");
        stmt.setTimestamp(1, Timestamp.from(endTime));
        stmt.setInt(2, id);
        stmt.executeUpdate();
        this.endTime = endTime;
    }

    public void endsNow() throws SQLException {
        setEndTime(Instant.now());
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void playerKilledMob(UUID uuid, Entity entity) {
        // not player
    }

    public void playerKilledMob(Player player, Entity entity) {
        playerKilledMob(player.getUniqueId(), entity);
    }

}
