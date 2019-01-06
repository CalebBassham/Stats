package me.calebbassham.stats;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("unused")
public class Game {

    private int id;
    private Instant startTime;
    private Instant endTime;

    private Game(int id, Instant startTime, Instant endTime) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    public static Game create() throws SQLException {
        var conn = Stats.getConnection();

        var update = conn.prepareStatement("INSERT INTO game (start_time) VALUES (?)");
        final var startTime = now();
        update.setTimestamp(1, startTime);
        update.executeUpdate();
        update.close();

        var query = conn.prepareStatement("SELECT LAST_INSERT_ID()");
        var rs = query.executeQuery();
        rs.first();
        var id = rs.getInt(1);
        rs.close();
        query.close();

        conn.close();

        return new Game(id, startTime.toInstant(), null);
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
        stmt.setTimestamp(4, now());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public void playerBrokeBlock(UUID player, String material) throws SQLException {
        var conn = Stats.getConnection();
        var stmt = conn.prepareStatement("INSERT INTO block_broken (game_id, player_id, block, time_broken) VALUES (?, ?, ?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, player.toString());
        stmt.setString(3, material);
        stmt.setTimestamp(4, now());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public void playerBrokeBlock(UUID player, Material material) throws SQLException {
        playerBrokeBlock(player, material.name());
    }

    private int saveItem(Connection conn, ItemStack item) throws SQLException {
        var ps = conn.prepareStatement("INSERT INTO item (type, amount, damage, name, lore) VALUES (?, ?, ?, ?, ?)");

        ps.setString(1, item.getType().name());
        ps.setInt(2, item.getAmount());

        var meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            var damageable = (Damageable) meta;
            if (damageable.hasDamage()) {
                ps.setInt(3, damageable.getDamage());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
        } else {
            ps.setNull(3, Types.INTEGER);
        }

        if (meta.hasDisplayName()) {
            ps.setString(4, meta.getDisplayName());
        } else {
            ps.setNull(4, Types.VARCHAR);
        }

        if (meta.hasLore()) {
            ps.setString(5, String.join("\n", meta.getLore()));
        } else {
            ps.setNull(5, Types.VARCHAR);
        }

        ps.executeUpdate();
        ps.close();

        ps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
        var rs = ps.executeQuery();
        rs.first();
        var itemId = rs.getInt(1);
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

        return itemId;
    }

    public int snapshotPlayerInventory(Connection conn, UUID player, HashMap<Integer, ItemStack> items) throws SQLException {
        var itemStmt = conn.prepareStatement("INSERT INTO item (type, amount, damage, name, lore) VALUES (?, ?, ?, ?, ?)");

        for (var slot : items.keySet()) {
            var item = items.get(slot);
            itemStmt.setString(1, item.getType().name());
            itemStmt.setInt(2, item.getAmount());

            var meta = item.getItemMeta();
            if (meta instanceof Damageable) {
                var damageable = (Damageable) meta;
                if (damageable.hasDamage()) {
                    itemStmt.setInt(3, damageable.getDamage());
                } else {
                    itemStmt.setNull(3, Types.INTEGER);
                }
            } else {
                itemStmt.setNull(3, Types.INTEGER);
            }

            if (meta.hasDisplayName()) {
                itemStmt.setString(4, meta.getDisplayName());
            } else {
                itemStmt.setNull(4, Types.VARCHAR);
            }

            if (meta.hasLore()) {
                itemStmt.setString(5, String.join("\n", meta.getLore()));
            } else {
                itemStmt.setNull(5, Types.VARCHAR);
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
            var itemId = itemsRs.getInt("id");
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
        invStmt.setTimestamp(3, now());
        invStmt.executeUpdate();
        invStmt.close();

        var getInvStmt = conn.prepareStatement("SELECT LAST_INSERT_ID()");
        var invRs = getInvStmt.executeQuery();
        invRs.first();
        var invId = invRs.getInt(1);
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

        return invId;
    }

    public int snapshotPlayerInventory(UUID player, HashMap<Integer, ItemStack> items) throws SQLException {
        var conn = Stats.getConnection();
        var id = snapshotPlayerInventory(Stats.getConnection(), player, items);
        conn.close();
        return id;
    }

    public int snapshotPlayerLocation(Connection conn, UUID player, int x, int y, int z) throws SQLException {
        var ps = conn.prepareStatement("INSERT INTO location (game_id, player_id, time_created, x, y, z) VALUES (?, ?, ?, ?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setTimestamp(3, now());
        ps.setInt(4, x);
        ps.setInt(5, y);
        ps.setInt(6, z);

        ps.executeUpdate();
        ps.close();

        ps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
        var rs = ps.executeQuery();
        rs.first();
        var id = rs.getInt(1);
        rs.close();

        ps.close();
        return id;
    }

    public int snapshotPlayerLocation(UUID player, int x, int y, int z) throws SQLException {
        var conn = Stats.getConnection();
        var id = snapshotPlayerLocation(Stats.getConnection(), player, x, y, z);
        conn.close();
        return id;
    }

    public void batchSnapshotPlayerLocation(Player[] players) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO location (game_id, player_id, time_created, x, y, z) VALUES (?, ?, ?, ?, ?, ?)");

        for (var player : players) {
            var location = player.getLocation();
            ps.setInt(1, id);
            ps.setString(2, player.getUniqueId().toString());
            ps.setInt(1, id);
            ps.setString(2, player.toString());
            ps.setTimestamp(3, now());
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

        var itemId = saveItem(conn, item);

        var ps = conn.prepareStatement("INSERT INTO fishing_item_caught (game_id, item_id, time_caught) VALUES (?, ?, ?)");
        ps.setInt(1, id);
        ps.setInt(2, itemId);
        ps.setTimestamp(3, now());

        ps.executeUpdate();
        ps.close();

        conn.close();
    }

    public void playerKilledPlayer(UUID killer, HashMap<Integer, ItemStack> killerItems, Location killerLocation, double killerHealth, double killerMaxHealth, UUID killed, HashMap<Integer, ItemStack> killedItems, Location killedLocation, int killedTotalExperience) throws SQLException {
        var conn = Stats.getConnection();

        var killerInventoryId = snapshotPlayerInventory(conn, killer, killerItems);
        var killedInventoryId = snapshotPlayerInventory(conn, killed, killedItems);

        var killerLocationId = snapshotPlayerLocation(conn, killer, killerLocation.getBlockX(), killerLocation.getBlockY(), killerLocation.getBlockZ());
        var killedLocationId = snapshotPlayerLocation(conn, killed, killedLocation.getBlockX(), killedLocation.getBlockY(), killedLocation.getBlockZ());

        var ps = conn.prepareStatement("INSERT INTO player_kill (game_id, killed_player_id, killed_player_inventory_id, killed_player_location_id, killed_player_experience," +
                "killer_player_id, killer_player_inventory_id, killer_player_location_id, killer_health_remaining, killer_max_health) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, killed.toString());
        ps.setInt(3, killedInventoryId);
        ps.setInt(4, killedLocationId);
        ps.setInt(5, killedTotalExperience);
        ps.setString(6, killer.toString());
        ps.setInt(7, killerInventoryId);
        ps.setInt(8, killerLocationId);
        ps.setDouble(9, killerHealth);
        ps.setDouble(10, killerMaxHealth);
        ps.executeUpdate();
        ps.close();

        conn.close();
    }

    public void playerStarted(UUID player) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO game_player (game_id, player_id, time_started) VALUES (?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setTimestamp(3, now());
        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playersStarted(UUID[] players) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO game_player (game_id, player_id, time_started) VALUES (?, ?, ?)");

        ps.setTimestamp(3, now());

        for (var player : players) {
            ps.setInt(1, id);
            ps.setString(2, player.toString());

            ps.addBatch();
        }

        ps.executeBatch();

        ps.close();
        conn.close();
    }

    public void playerEnchantedItem(UUID player) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("UPDATE game_player SET items_enchanted = items_enchanted + 1 WHERE game_id = ? AND player_id = ?");

        ps.setInt(1, id);
        ps.setString(2, player.toString());

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerWon(UUID player) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO game_winner (game_id, player_id) VALUES (?, ?)");

        ps.setInt(1, id);
        ps.setString(2, player.toString());

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playersWon(UUID[] players) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO game_winner (game_id, player_id) VALUES (?, ?)");

        for (var player : players) {
            ps.setInt(1, id);
            ps.setString(2, player.toString());
            ps.addBatch();
        }

        ps.executeBatch();

        ps.close();
        conn.close();
    }

    public void playerShotBow(UUID player, boolean hit, double distance) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO bow_shot (game_id, player_id, hit, distance, time_shot) VALUES (?, ?, ?, ?, ?)");

        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setBoolean(3, hit);
        ps.setDouble(4, distance);
        ps.setTimestamp(5, now());

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerTamedEntity(UUID player, EntityType mob) throws SQLException {
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO mob_tamed (game_id, player_id, mob, time_tamed) VALUES (?, ?, ?, ?)");

        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setString(3, mob.name());
        ps.setTimestamp(4, now());

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerDamageTaken(UUID damagedPlayer, EntityDamageEvent.DamageCause damageCause, double damageTaken, double damagedPlayerHealth, double damagedPlayerMaxHealth) throws SQLException {
        if (damageTaken <= 0) return;
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO damage_taken (game_id, time_taken, damage_cause, damage_taken, damaged_player_id, damaged_player_health, damaged_player_max_health) VALUES (?, ?, ?, ?, ?, ?, ?)");

        ps.setInt(1, id);
        ps.setTimestamp(2, now());
        ps.setString(3, damageCause.name());
        ps.setDouble(4, damageTaken);
        ps.setString(5, damagedPlayer.toString());
        ps.setDouble(6, damagedPlayerHealth);
        ps.setDouble(7, damagedPlayerMaxHealth);

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerDamageTaken(UUID damagedPlayer, EntityDamageEvent.DamageCause damageCause, double damageTaken, double damagedPlayerHealth, double damagedPlayerMaxHealth, EntityType damagingEntity, double damagingEntityHealth, double damagingEntityMaxHealth) throws SQLException {
        if (damageTaken <= 0) return;
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO damage_taken (game_id, time_taken, damage_cause, damage_taken, damaged_player_id, damaged_player_health, damaged_player_max_health, damaging_mob, damaging_mob_health, damaging_mob_max_health) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        ps.setInt(1, id);
        ps.setTimestamp(2, now());
        ps.setString(3, damageCause.name());
        ps.setDouble(4, damageTaken);
        ps.setString(5, damagedPlayer.toString());
        ps.setDouble(6, damagedPlayerHealth);
        ps.setDouble(7, damagedPlayerMaxHealth);
        ps.setString(8, damagingEntity.name());
        ps.setDouble(9, damagingEntityHealth);
        ps.setDouble(10, damagingEntityMaxHealth);

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerDamageTaken(UUID damagedPlayer, EntityDamageEvent.DamageCause damageCause, double damageTaken, double damagedPlayerHealth, double damagedPlayerMaxHealth, UUID damagingPlayer, double damagingPlayerHealth, double damagingPlayerMaxHealth) throws SQLException {
        if (damageTaken <= 0) return;
        var conn = Stats.getConnection();
        var ps = conn.prepareStatement("INSERT INTO damage_taken (game_id, time_taken, damage_cause, damage_taken, damaged_player_id, damaged_player_health, damaged_player_max_health, damaging_mob, damaging_mob_health, damaging_mob_max_health, damaging_player_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        ps.setInt(1, id);
        ps.setTimestamp(2, now());
        ps.setString(3, damageCause.name());
        ps.setDouble(4, damageTaken);
        ps.setString(5, damagedPlayer.toString());
        ps.setDouble(6, damagedPlayerHealth);
        ps.setDouble(7, damagedPlayerMaxHealth);
        ps.setString(8, EntityType.PLAYER.name());
        ps.setDouble(9, damagingPlayerHealth);
        ps.setDouble(10, damagingPlayerMaxHealth);
        ps.setString(11, damagingPlayer.toString());

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerConsumeItem(UUID player, ItemStack item) throws SQLException {
        var conn = Stats.getConnection();

        var ps = conn.prepareStatement("INSERT INTO item (type, amount, damage, name, lore) VALUES (?, ?, ?, ?, ?)");

        ps.setString(1, item.getType().name());
        ps.setInt(2, 1);

        var meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            var damageable = (Damageable) meta;
            if (damageable.hasDamage()) {
                ps.setInt(3, damageable.getDamage());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
        } else {
            ps.setNull(3, Types.INTEGER);
        }

        if (meta.hasDisplayName()) {
            ps.setString(4, meta.getDisplayName());
        } else {
            ps.setNull(4, Types.VARCHAR);
        }

        if (meta.hasLore()) {
            ps.setString(5, String.join("\n", meta.getLore()));
        } else {
            ps.setNull(5, Types.VARCHAR);
        }

        ps.executeUpdate();
        ps.close();

        ps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
        var rs = ps.executeQuery();

        rs.first();
        var itemId = rs.getInt(1);

        rs.close();
        ps.close();

        ps = conn.prepareStatement("INSERT INTO item_consume (game_id, player_id, time_consumed, item_id) VALUES (?, ?, ?, ?)");

        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setTimestamp(3, now());
        ps.setInt(4, itemId);

        ps.executeUpdate();

        ps.close();
        conn.close();
    }

    public void playerCraftedItem(Connection conn, UUID player, ItemStack item) throws SQLException {
        var itemId = saveItem(conn, item);
        var ps = conn.prepareStatement("INSERT INTO craft_item (game_id, player_id, time_crafted, item_id) VALUES (?, ?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, player.toString());
        ps.setTimestamp(3, now());
        ps.setInt(4, itemId);
        ps.executeUpdate();
        ps.close();
    }

    public void playerCraftedItem(UUID player, ItemStack item) throws SQLException {
        var conn = Stats.getConnection();
        playerCraftedItem(conn, player, item);
        conn.close();
    }

}
