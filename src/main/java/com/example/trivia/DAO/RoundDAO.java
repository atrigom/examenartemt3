package com.example.trivia.DAO;
import com.example.trivia.model.Round;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RoundDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public RoundDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM rounds WHERE round_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public Optional<Round> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM rounds WHERE round_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Round r = mapRowToRound(rs);
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public Round save(Round r) throws SQLException {
        boolean insert = r.getRoundId() == null;
        String sql = insert
                ? "INSERT INTO rounds (game_id, round_number, created_at, ended_at) VALUES (?, ?, ?, ?)"
                : "UPDATE rounds SET game_id = ?, round_number = ?, created_at = ?, ended_at = ? WHERE round_id = ?";

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, r.getGameId());
        stmt.setInt(2, r.getRoundNumber());
        stmt.setTimestamp(3, r.getCreatedAt() != null ? Timestamp.from(r.getCreatedAt()) : null);
        stmt.setTimestamp(4, r.getEndedAt() != null ? Timestamp.from(r.getEndedAt()) : null);

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setRoundId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(5, r.getRoundId());
            stmt.executeUpdate();
        }
        return r;
    }

    private Round mapRowToRound(ResultSet rs) throws SQLException {
        Round r = new Round();
        r.setRoundId(rs.getString("round_id"));
        r.setGameId(rs.getString("game_id"));
        r.setRoundNumber(rs.getInt("round_number"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp endedAt = rs.getTimestamp("ended_at");
        r.setCreatedAt(createdAt != null ? createdAt.toInstant() : null);
        r.setEndedAt(endedAt != null ? endedAt.toInstant() : null);
        return r;
    }

    public List<Round> findByGameId(String gameId) throws SQLException {
        List<Round> rounds = new ArrayList<>();
        String sql = "SELECT * FROM rounds WHERE game_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, gameId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Round r = mapRowToRound(rs);
                rounds.add(r);
            }
        }
        return rounds;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
