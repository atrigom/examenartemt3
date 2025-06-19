package com.example.trivia.DAO;
import com.example.trivia.model.Settings;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class SettingsDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public SettingsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM settings WHERE settings_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public Optional<Settings> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM settings WHERE settings_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Settings s = mapRowToSettings(rs);
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public Settings save(Settings s) throws SQLException {
        boolean insert = s.getSettingsId() == null;
        String sql = insert
                ? "INSERT INTO settings (rounds, time_per_round, questions_per_round, difficulty, max_players_per_team) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE settings SET rounds = ?, time_per_round = ?, questions_per_round = ?, difficulty = ?, max_players_per_team = ? WHERE settings_id = ?";

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, s.getRounds());
        stmt.setInt(2, s.getTimePerRound());
        stmt.setInt(3, s.getQuestionsPerRound());
        stmt.setString(4, s.getDifficulty());
        stmt.setInt(5, s.getMaxPlayersPerTeam());

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setSettingsId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(6, s.getSettingsId());
            stmt.executeUpdate();
        }
        return s;
    }

    private Settings mapRowToSettings(ResultSet rs) throws SQLException {
        Settings s = new Settings();
        s.setSettingsId(rs.getString("settings_id"));
        s.setRounds(rs.getInt("rounds"));
        s.setTimePerRound(rs.getInt("time_per_round"));
        s.setQuestionsPerRound(rs.getInt("questions_per_round"));
        s.setDifficulty(rs.getString("difficulty"));
        s.setMaxPlayersPerTeam(rs.getInt("max_players_per_team"));
        return s;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}