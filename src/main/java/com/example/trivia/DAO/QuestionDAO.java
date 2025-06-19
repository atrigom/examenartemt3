package com.example.trivia.DAO;
import com.example.trivia.model.Question;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Arrays;

@Repository
public class QuestionDAO {
    private final DataSource dataSource;
    private final Connection connection;

    public QuestionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            this.connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM questions WHERE question_id = ?");
        stmt.setString(1, id);
        stmt.executeUpdate();
    }

    public Optional<Question> findById(String id) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM questions WHERE question_id = ?");
        stmt.setString(1, id);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Question q = mapRowToQuestion(rs);
                return Optional.of(q);
            }
        }
        return Optional.empty();
    }

    public Question save(Question q) throws SQLException {
        boolean insert = q.getQuestionId() == null;
        String sql = insert ?
                "INSERT INTO questions (round_id, type, text, media_url, options, correct_answers, points) VALUES (?, ?, ?, ?, ?, ?, ?)" :
                "UPDATE questions SET round_id = ?, type = ?, text = ?, media_url = ?, options = ?, correct_answers = ?, points = ? WHERE question_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, q.getRoundId());
        stmt.setString(2, q.getType());
        stmt.setString(3, q.getText());
        stmt.setString(4, q.getMediaUrl());
        stmt.setString(5, listToString(q.getOptions()));
        stmt.setString(6, listToString(q.getCorrectAnswers()));
        stmt.setInt(7, q.getPoints());

        if (insert) {
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    q.setQuestionId(keys.getString(1));
                }
            }
        } else {
            stmt.setString(8, q.getQuestionId());
            stmt.executeUpdate();
        }
        return q;
    }

    private Question mapRowToQuestion(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setQuestionId(rs.getString("question_id"));
        q.setRoundId(rs.getString("round_id"));
        q.setType(rs.getString("type"));
        q.setText(rs.getString("text"));
        q.setMediaUrl(rs.getString("media_url"));
        q.setOptions(stringToList(rs.getString("options")));
        q.setCorrectAnswers(stringToList(rs.getString("correct_answers")));
        q.setPoints(rs.getInt("points"));
        return q;
    }

    private String listToString(List<String> list) {
        return (list == null || list.isEmpty()) ? null : String.join(",", list);
    }

    private List<String> stringToList(String str) {
        return (str == null || str.isEmpty()) ? new ArrayList<>() : Arrays.asList(str.split(","));
    }


    public List<Question> findByRoundId(String roundId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE round_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, roundId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Question q = mapRowToQuestion(rs);
                questions.add(q);
            }
        }
        return questions;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}