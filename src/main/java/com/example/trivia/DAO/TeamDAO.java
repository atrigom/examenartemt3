package com.example.trivia.DAO;
import com.example.trivia.model.Team;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TeamDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public TeamDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM teams WHERE team_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public List<Team> findByRoomId(String roomId) throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE room_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, roomId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Team t = mapRowToTeam(rs);
                teams.add(t);
            }
        }

        return teams;
    }

    public Optional<Team> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM teams WHERE team_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Team t = mapRowToTeam(rs);
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public Team save(Team t) throws SQLException {
        boolean insert = t.getTeamId() == null;
        String sql = insert
                ? "INSERT INTO teams (room_id) VALUES (?)"
                : "UPDATE teams SET room_id = ? WHERE team_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, t.getRoomId());

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setTeamId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(2, t.getTeamId());
            stmt.executeUpdate();
        }
        return t;
    }

    private Team mapRowToTeam(ResultSet rs) throws SQLException {
        Team t = new Team();
        t.setTeamId(rs.getString("team_id"));
        t.setRoomId(rs.getString("room_id"));
        return t;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}