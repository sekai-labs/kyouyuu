package com.sekailabs.kyouyuu.storage;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.storage.query.QueryBuilder;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ChannelRepository {

    private final DatabaseManager databaseManager;

    public void insert(Channel channel) throws SQLException {
        String sql = QueryBuilder.insert()
                .table("channels")
                .column("id")
                .column("name")
                .column("size")
                .column("created_at")
                .column("updated_at")
                .column("inventory_data")
                .value("?")
                .value("?")
                .value("?")
                .value("?")
                .value("?")
                .value("NULL")
                .build()
                .build(databaseManager.getDatabaseType());

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel.id());
            stmt.setString(2, channel.name());
            stmt.setInt(3, channel.size());
            stmt.setLong(4, channel.createdAt().toEpochMilli());
            stmt.setLong(5, channel.updatedAt().toEpochMilli());
            stmt.executeUpdate();
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    public Optional<Channel> findById(String id) throws SQLException {
        String sql = QueryBuilder.select()
                .table("channels")
                .column("id")
                .column("name")
                .column("size")
                .column("created_at")
                .column("updated_at")
                .where("id = ?")
                .build()
                .build();

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id.toLowerCase().trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToChannel(rs));
                }
            }
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
        return Optional.empty();
    }

    public List<Channel> findAll() throws SQLException {
        List<Channel> channels = new ArrayList<>();
        String sql = QueryBuilder.select()
                .table("channels")
                .column("id")
                .column("name")
                .column("size")
                .column("created_at")
                .column("updated_at")
                .orderBy("id ASC")
                .build()
                .build();

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                channels.add(mapResultSetToChannel(rs));
            }
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
        return channels;
    }

    public void update(Channel channel) throws SQLException {
        String sql = QueryBuilder.update()
                .table("channels")
                .set("name = ?")
                .set("size = ?")
                .set("updated_at = ?")
                .where("id = ?")
                .build()
                .build();

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel.name());
            stmt.setInt(2, channel.size());
            stmt.setLong(3, channel.updatedAt().toEpochMilli());
            stmt.setString(4, channel.id());
            stmt.executeUpdate();
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    public void saveInventoryData(String channelId, byte[] data) throws SQLException {
        String sql = QueryBuilder.update()
                .table("channels")
                .set("inventory_data = ?")
                .set("updated_at = ?")
                .where("id = ?")
                .build()
                .build();

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBytes(1, data);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, channelId.toLowerCase().trim());
            stmt.executeUpdate();
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    public byte[] loadInventoryData(String channelId) throws SQLException {
        String sql = QueryBuilder.select()
                .table("channels")
                .column("inventory_data")
                .where("id = ?")
                .build()
                .build();

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId.toLowerCase().trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("inventory_data");
                }
            }
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
        return null;
    }

    public boolean delete(String id) throws SQLException {
        String sql = QueryBuilder.delete()
                .table("channels")
                .where("id = ?")
                .build()
                .build();

        Connection conn = databaseManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id.toLowerCase().trim());
            return stmt.executeUpdate() > 0;
        } finally {
            if (databaseManager.getDatabaseType() != DatabaseType.SQLITE && conn != null) {
                conn.close();
            }
        }
    }

    private Channel mapResultSetToChannel(ResultSet rs) throws SQLException {
        return Channel.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .size(rs.getInt("size"))
                .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                .updatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")))
                .build();
    }
}
