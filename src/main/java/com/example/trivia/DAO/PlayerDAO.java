package com.example.trivia.DAO;

import com.example.trivia.model.Player;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PlayerDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public PlayerDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM players WHERE player_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public List<Player> findByRoomId(String roomId) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE room_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, roomId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Player p = mapRowToPlayer(rs);
                players.add(p);
            }
        }
        return players;
    }

    public List<Player> findByTeamId(String teamId) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE team_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, teamId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Player p = mapRowToPlayer(rs);
                players.add(p);
            }
        }
        return players;
    }

    public Optional<Player> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM players WHERE player_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Player p = mapRowToPlayer(rs);
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Player save(Player p) throws SQLException {
        boolean insert = p.getPlayerId() == null;
        String sql = insert
                ? "INSERT INTO players (room_id, username, host, team_id) VALUES (?, ?, ?, ?)"
                : "UPDATE players SET room_id = ?, username = ?, host = ?, team_id = ? WHERE player_id = ?";

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, p.getRoomId());
        stmt.setString(2, p.getUsername());
        stmt.setBoolean(3, p.isHost());
        stmt.setString(4, p.getTeamId());

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setPlayerId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(5, p.getPlayerId());
            stmt.executeUpdate();
        }
        return p;
    }

    private Player mapRowToPlayer(ResultSet rs) throws SQLException {
        Player p = new Player();
        p.setPlayerId(rs.getString("player_id"));
        p.setRoomId(rs.getString("room_id"));
        p.setUsername(rs.getString("username"));
        p.setHost(rs.getBoolean("host"));
        p.setTeamId(rs.getString("team_id"));
        return p;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}