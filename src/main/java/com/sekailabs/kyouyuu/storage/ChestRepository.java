package com.sekailabs.kyouyuu.storage;

import com.sekailabs.kyouyuu.model.ChestLocation;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.storage.query.QueryBuilder;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ChestRepository {

    private final DatabaseManager databaseManager;

    public void link(LinkedChest chest) throws SQLException {
        DatabaseType type = databaseManager.getDatabaseType();
        String sql = QueryBuilder.insert()
                .table("linked_chests")
                .column("world_name")
                .column("world_uid")
                .column("x")
                .column("y")
                .column("z")
                .column("channel_id")
                .conflictKey("world_name")
                .conflictKey("x")
                .conflictKey("y")
                .conflictKey("z")
                .updateColumn("world_uid")
                .updateColumn("channel_id")
                .build()
                .build(type);

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chest.worldName());
            stmt.setString(2, chest.worldUid() != null ? chest.worldUid().toString() : null);
            stmt.setInt(3, chest.x());
            stmt.setInt(4, chest.y());
            stmt.setInt(5, chest.z());
            stmt.setString(6, chest.channelId());
            stmt.executeUpdate();
        } finally {
            if (type != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    public boolean unlink(String worldName, int x, int y, int z) throws SQLException {
        String sql = QueryBuilder.delete()
                .table("linked_chests")
                .where("world_name = ? AND x = ? AND y = ? AND z = ?")
                .build()
                .build();
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, worldName);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            return stmt.executeUpdate() > 0;
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    public Optional<LinkedChest> findByLocation(String worldName, int x, int y, int z) throws SQLException {
        String sql = QueryBuilder.select()
                .table("linked_chests")
                .column("world_name")
                .column("world_uid")
                .column("x")
                .column("y")
                .column("z")
                .column("channel_id")
                .where("world_name = ? AND x = ? AND y = ? AND z = ?")
                .build()
                .build();
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, worldName);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLinkedChest(rs));
                }
            }
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
        return Optional.empty();
    }

    public Optional<LinkedChest> findByLocation(ChestLocation location) throws SQLException {
        return findByLocation(location.worldName(), location.x(), location.y(), location.z());
    }

    public List<LinkedChest> findByChannel(String channelId) throws SQLException {
        List<LinkedChest> chests = new ArrayList<>();
        String sql = QueryBuilder.select()
                .table("linked_chests")
                .column("world_name")
                .column("world_uid")
                .column("x")
                .column("y")
                .column("z")
                .column("channel_id")
                .where("channel_id = ?")
                .orderBy("world_name ASC, x ASC, y ASC, z ASC")
                .build()
                .build();
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId.toLowerCase().trim());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chests.add(mapResultSetToLinkedChest(rs));
                }
            }
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
        return chests;
    }

    public List<LinkedChest> findAll() throws SQLException {
        List<LinkedChest> chests = new ArrayList<>();
        String sql = QueryBuilder.select()
                .table("linked_chests")
                .column("world_name")
                .column("world_uid")
                .column("x")
                .column("y")
                .column("z")
                .column("channel_id")
                .build()
                .build();
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                chests.add(mapResultSetToLinkedChest(rs));
            }
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
        return chests;
    }

    public int deleteByChannel(String channelId) throws SQLException {
        String sql = QueryBuilder.delete()
                .table("linked_chests")
                .where("channel_id = ?")
                .build()
                .build();
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId.toLowerCase().trim());
            return stmt.executeUpdate();
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    private LinkedChest mapResultSetToLinkedChest(ResultSet rs) throws SQLException {
        String uidStr = rs.getString("world_uid");
        UUID uid = uidStr != null && !uidStr.isBlank() ? UUID.fromString(uidStr) : null;
        return LinkedChest.builder()
                .channelId(rs.getString("channel_id"))
                .worldName(rs.getString("world_name"))
                .worldUid(uid)
                .x(rs.getInt("x"))
                .y(rs.getInt("y"))
                .z(rs.getInt("z"))
                .build();
    }
}
