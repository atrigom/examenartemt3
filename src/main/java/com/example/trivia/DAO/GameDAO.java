package com.example.trivia.DAO;

import com.example.trivia.model.Game;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

@Repository
public class GameDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public GameDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM games WHERE game_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public Optional<Game> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM games WHERE game_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Game g = mapRowToGame(rs);
                return Optional.of(g);
            }
        }
        return Optional.empty();
    }

    public Game save(Game g) throws SQLException {
        boolean insert = g.getGameId() == null;
        String sql = insert
                ? "INSERT INTO games (room_id, created_at, ended_at, settings_id) VALUES (?, ?, ?, ?)"
                : "UPDATE games SET room_id = ?, created_at = ?, ended_at = ?, settings_id = ? WHERE game_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, g.getRoomId());
        stmt.setTimestamp(2, g.getCreatedAt() != null ? Timestamp.from(g.getCreatedAt()) : null);
        stmt.setTimestamp(3, g.getEndedAt() != null ? Timestamp.from(g.getEndedAt()) : null);
        stmt.setString(4, g.getSettingsId());

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    g.setGameId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(5, g.getGameId());
            stmt.executeUpdate();
        }
        return g;
    }

    private Game mapRowToGame(ResultSet rs) throws SQLException {
        Game g = new Game();
        g.setGameId(rs.getString("game_id"));
        g.setRoomId(rs.getString("room_id"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp endedAt = rs.getTimestamp("ended_at");
        g.setCreatedAt(createdAt != null ? createdAt.toInstant() : null);
        g.setEndedAt(endedAt != null ? endedAt.toInstant() : null);
        g.setSettingsId(rs.getString("settings_id"));
        return g;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}

