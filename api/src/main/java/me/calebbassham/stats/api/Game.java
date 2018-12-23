package me.calebbassham.stats.api;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
        delete(id);
    }

    public static void delete(int id) throws SQLException {
        var conn = Stats.getConnection();
        var query = conn.prepareStatement("DELETE FROM game WHERE id = ?");
        query.setInt(1, id);
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

    public void playerKilledMob(UUID uuid, EntityType entity) throws SQLException {
        var conn = Stats.getConnection();
        var stmt = conn.prepareStatement("INSERT INTO mob_kill (game_id, player_id, mob, time_killed) VALUES (?, ?, ?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, uuid.toString());
        stmt.setString(3, entity.name());
        stmt.setTimestamp(4, Timestamp.from(Instant.now()));
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public void playerBrokeBlock(UUID uuid, Material material) throws SQLException {
        var conn = Stats.getConnection();
        var stmt = conn.prepareStatement("INSERT INTO block_broken (game_id, player_id, block, time_broken) VALUES (?, ?, ?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, uuid.toString());
        stmt.setString(3, material.name());
        stmt.setTimestamp(4, Timestamp.from(Instant.now()));
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public void snapshotPlayerInventory(PlayerInventory inv) throws SQLException {
        var player = inv.getHolder().getUniqueId();

        // Slot
        var items = new HashMap<Integer, ItemStack>();
        for (var slot = 0; slot < 40; slot++) {
            var item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) continue;
            items.put(slot, item);
        }

        var conn = Stats.getConnection();
        var itemStmt = conn.prepareStatement("INSERT INTO item (type, amount, durability) VALUES (?, ?, ?)");

        for (var slot : items.keySet()) {
            var item = items.get(slot);
            itemStmt.setString(1, item.getType().name());
            itemStmt.setInt(2, item.getAmount());

            var meta = item.getItemMeta();
            if (meta instanceof Damageable) {
                var damageable = (Damageable) meta;
                itemStmt.setInt(3, damageable.getDamage());
            } else {
                itemStmt.setNull(3, Types.INTEGER);
            }

            itemStmt.addBatch();
            itemStmt.clearParameters();
        }

        var numberOfItems = itemStmt.executeBatch();
        itemStmt.close();

        var getItemsStmt = conn.prepareStatement("SELECT id FROM item ORDER BY id DESC LIMIT ?");
        getItemsStmt.setInt(1, numberOfItems.length);
        var itemsRs = getItemsStmt.executeQuery();

        var enchantmentStmt = conn.prepareStatement("INSERT INTO enchantment (item_id, enchantment, enchantment_level) VALUES (?, ?, ?)");
        while (itemsRs.next()) {
            var itemId = itemsRs.getInt("item_id");
            var item = new ArrayList<>(items.values()).get(itemsRs.getRow() - 1);

            for (var enchantment : item.getEnchantments().keySet()) {
                var level = item.getEnchantmentLevel(enchantment);
                enchantmentStmt.setInt(1, itemId);
                enchantmentStmt.setString(2, enchantment.getName());
                enchantmentStmt.setInt(3, level);
                enchantmentStmt.addBatch();
                enchantmentStmt.clearParameters();
            }
        }

        enchantmentStmt.executeBatch();
        enchantmentStmt.close();

        var invStmt = conn.prepareStatement("INSERT INTO inventory (player_id, game_id, time_created) VALUES (?, ?, ?)");
        invStmt.setString(1, player.toString());
        invStmt.setInt(2, id);
        invStmt.setTimestamp(3, Timestamp.from(Instant.now()));
        invStmt.executeUpdate();
        invStmt.close();

        var getInvStmt = conn.prepareStatement("SELECT id FROM inventory ORDER BY id DESC LIMIT 1");
        var invRs = getInvStmt.executeQuery();
        invRs.next();
        var invId = invRs.getInt("id");
        invRs.close();

        var invItemStmt = conn.prepareStatement("INSERT INTO inventory_item (inventory_id, item_id, slot) VALUES (?, ?, ?)");

        itemsRs.beforeFirst();
        while (itemsRs.next()) {
            var itemId = itemsRs.getInt("id");
            var slot = new ArrayList<>(items.keySet()).get(itemsRs.getRow() - 1);
            invItemStmt.setInt(1, invId);
            invItemStmt.setInt(2, itemId);
            invItemStmt.setInt(3, slot);
            invItemStmt.addBatch();
            invItemStmt.clearParameters();
        }

        itemsRs.close();
        getItemsStmt.close();

        invItemStmt.executeBatch();
        invItemStmt.close();

        conn.close();
    }

    public void snapshotPlayerLocation(UUID player, int x, int y, int z) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareCall("INSERT INTO location (game_id, player_id, time_created, x, y, z) VALUES (?, ?, ?, ?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setTimestamp(3, Timestamp.from(Instant.now()));
        ps.setInt(4, x);
        ps.setInt(5, y);
        ps.setInt(6, z);

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void batchSnapshotPlayerLocation(Player[] players) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareCall("INSERT INTO location (game_id, player_id, time_created, x, y, z) VALUES (?, ?, ?, ?, ?, ?)");

        for (var player : players) {
            var location = player.getLocation();
            ps.setInt(1, id);
            ps.setString(2, player.getUniqueId().toString());
            ps.setInt(1, id);
            ps.setString(2, player.toString());
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setInt(4, location.getBlockX());
            ps.setInt(5, location.getBlockY());
            ps.setInt(6, location.getBlockZ());
            ps.addBatch();
            ps.clearParameters();
        }

        ps.executeBatch();

        ps.close();
        conn.close();
    }

    public void playerCaughtFishingItem(UUID player, ItemStack item) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO item (type, amount) VALUES (?, ?)");

        ps.setString(1, item.getType().name());
        ps.setInt(2, 1);

        ps.executeUpdate();
        ps.close();

        ps = conn.prepareStatement("SELECT id FROM item ORDER BY id DESC LIMIT 1");
        var rs = ps.executeQuery();

        rs.next();
        var itemId = rs.getInt(id);

        rs.close();
        ps.close();

        if (item.getEnchantments().size() > 0) {
            ps = conn.prepareStatement("INSERT INTO enchantment (item_id, enchantment, enchantment_level) VALUES (?, ?, ?)");

            for (var enchantment : item.getEnchantments().keySet()) {
                var level = item.getEnchantmentLevel(enchantment);
                ps.setInt(1, itemId);
                ps.setString(2, enchantment.getName());
                ps.setInt(3, level);
                ps.addBatch();
                ps.clearParameters();
            }

            ps.executeBatch();
            ps.close();
        }

        ps = conn.prepareStatement("INSERT INTO fishing_item_caught (game_id, item_id, time_caught) VALUES (?, ?, ?)");
        ps.setInt(1, id);
        ps.setInt(2, itemId);
        ps.setTimestamp(3, Timestamp.from(Instant.now()));

        ps.executeUpdate();
        ps.close();

        conn.close();
    }

}
