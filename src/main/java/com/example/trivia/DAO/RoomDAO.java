package com.example.trivia.DAO;
import com.example.trivia.model.Room;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

@Repository
public class RoomDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public RoomDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM rooms WHERE room_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public Optional<Room> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM rooms WHERE room_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Room r = mapRowToRoom(rs);
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public Room save(Room r) throws SQLException {
        boolean insert = r.getRoomId() == null;
        String sql = insert
                ? "INSERT INTO rooms (created_at, settings_id) VALUES (?, ?)"
                : "UPDATE rooms SET created_at = ?, settings_id = ? WHERE room_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setTimestamp(1, r.getCreatedAt() != null ? Timestamp.from(r.getCreatedAt()) : null);
        stmt.setString(2, r.getSettingsId());

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setRoomId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(3, r.getRoomId());
            stmt.executeUpdate();
        }
        return r;
    }

    private Room mapRowToRoom(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getString("room_id"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        r.setCreatedAt(createdAt != null ? createdAt.toInstant() : null);
        r.setSettingsId(rs.getString("settings_id"));
        return r;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}