package com.example.trivia.DAO;

import com.example.trivia.model.Answer;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class AnswerDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public AnswerDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM answers WHERE answer_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public Optional<Answer> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM answers WHERE answer_id = ?");
        stmt.setString(1, id);
        return Optional.empty();
    }

    public Answer save(Answer a) throws SQLException {
        boolean insert = a.getAnswerId() == null;
        String sql = insert
                ? "INSERT INTO answers (question_id, player_id, answer, correct) VALUES (?, ?, ?, ?)"
                : "UPDATE answers SET question_id = ?, player_id = ?, answer = ?, correct = ? WHERE answer_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, a.getQuestionId());
        stmt.setString(2, a.getPlayerId());
        stmt.setString(3, a.getAnswer());
        stmt.setBoolean(4, a.isCorrect() != null ? a.isCorrect() : false);
        return a;
    }

    private Answer mapRowToAnswer(ResultSet rs) throws SQLException {
        Answer a = new Answer();
        a.setAnswerId(rs.getString("answer_id"));
        a.setQuestionId(rs.getString("question_id"));
        a.setPlayerId(rs.getString("player_id"));
        a.setAnswer(rs.getString("answer"));
        a.setCorrect(rs.getBoolean("correct"));
        return a;
    }

    public Optional<Answer> findByQuestionIdAndPlayerId(String questionId, String playerId) throws SQLException {
        String sql = "SELECT * FROM answers WHERE question_id = ? AND player_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, questionId);
        stmt.setString(2, playerId);
        return Optional.empty();
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}