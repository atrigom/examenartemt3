package com.example.trivia.controller;

import com.example.trivia.DAO.*;
import com.example.trivia.model.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
public class GameController {
    private final GameDAO gameDAO;
    private final RoomDAO roomDAO;
    private final RoundDAO roundDAO;
    private final QuestionDAO questionDAO;
    private final PlayerDAO playerDAO;
    private final AnswerDAO answerDAO;
    private final SettingsDAO settingsDAO;

    public GameController(GameDAO gameDAO, RoomDAO roomDAO, RoundDAO roundDAO,
                QuestionDAO questionDAO, PlayerDAO playerDAO,
                          AnswerDAO answerDAO, SettingsDAO settingsDAO, TeamDAO teamDAO) {
        this.gameDAO = gameDAO;
        this.roomDAO = roomDAO;
        this.roundDAO = roundDAO;
        this.questionDAO = questionDAO;
        this.playerDAO = playerDAO;
        this.answerDAO = answerDAO;
        this.settingsDAO = settingsDAO;
    }

    @PostMapping("/games")
    public ResponseEntity<Game> createGame(@RequestBody Map<String, Object> body,
            HttpSession session) throws SQLException {
        String roomId = (String) body.get("roomId");
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = (Player) session.getAttribute(roomId);
        if (player == null || !player.isHost()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Room room = roomOptional.get();
        Optional<Settings> settingsOptional = settingsDAO.findById(room.getSettingsId());
        Settings settings = settingsOptional.get();

        Game game = new Game();
        game.setGameId(UUID.randomUUID().toString());
        game.setRoomId(roomId);
        game.setCreatedAt(Instant.now());
        game.setEndedAt(Instant.now().plus(
                Duration.ofSeconds(settings.getRounds() * settings.getTimePerRound())));
        game.setSettingsId(room.getSettingsId()); // Game uses Room's settings
        gameDAO.save(game);

        // Create rounds for the game, based on the game's settings
        for (int roundNumber = 1; roundNumber <= settings.getRounds(); roundNumber++) {
            Round round = new Round();
            round.setRoundId(UUID.randomUUID().toString());
            round.setGameId(game.getGameId());
            round.setRoundNumber(roundNumber);
            round.setCreatedAt(Instant.now().plus(
                    Duration.ofSeconds(settings.getTimePerRound() * (roundNumber - 1))));
            round.setEndedAt(round.getCreatedAt().plus(
                    Duration.ofSeconds(settings.getTimePerRound())));
            roundDAO.save(round);
        }

        URI location = URI.create("/games/" + game.getGameId());
        return ResponseEntity.created(location).body(game);
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<Game> getGame(@PathVariable String gameId) throws SQLException {
        Optional<Game> gameOptional = gameDAO.findById(gameId);
        if (gameOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Game game = gameOptional.get();
        return ResponseEntity.ok(game);
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable String gameId, HttpSession session) throws SQLException {
        Optional<Game> gameOptional = gameDAO.findById(gameId);
        if (gameOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Game game = gameOptional.get();
        Player player = (Player) session.getAttribute(game.getRoomId());
        if (player == null || !player.isHost()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        gameDAO.deleteById(gameId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/games/{gameId}/rounds")
    public ResponseEntity<List<Round>> getRounds(@PathVariable String gameId) throws SQLException {
        Optional<Game> gameOptional = gameDAO.findById(gameId);
        if (gameOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Round> rounds = roundDAO.findByGameId(gameId);
        return ResponseEntity.ok(rounds);
    }

    @GetMapping("/games/{gameId}/rounds/{roundId}/questions")
    public ResponseEntity<List<Question>> getRoundQuestions(@PathVariable String gameId,
            @PathVariable String roundId) throws SQLException {
        Optional<Game> gameOptional = gameDAO.findById(gameId);
        Optional<Round> roundOptional = roundDAO.findById(roundId);
        if (gameOptional.isEmpty() || roundOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Round round = roundOptional.get();
        if (Instant.now().isBefore(round.getCreatedAt())) {
            return ResponseEntity.badRequest().build(); // Round has not started yet
        }

        List<Question> questions = questionDAO.findByRoundId(roundId);

        // MUST NOT show the correct answers here!
        for (Question question : questions) {
            question.setCorrectAnswers(null);
        }

        return ResponseEntity.ok(questions);
    }

    @PostMapping("/games/{gameId}/rounds/{roundId}/questions/{questionId}/players/{playerId}")
    public ResponseEntity<Void> submitAnswer(@PathVariable String gameId,
            @PathVariable String roundId,
            @PathVariable String questionId,
            @PathVariable String playerId,
            @RequestBody Map<String, Object> body,
            HttpSession session) throws SQLException {
        Optional<Game> gameOptional = gameDAO.findById(gameId);
        Optional<Player> playerOptional = playerDAO.findById(playerId);
        Optional<Question> questionOptional = questionDAO.findById(questionId);
        Optional<Round> roundOptional = roundDAO.findById(roundId);
        if (gameOptional.isEmpty()
                || playerOptional.isEmpty()
                || questionOptional.isEmpty()
                || roundOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Round round = roundOptional.get();
        if (Instant.now().isAfter(round.getEndedAt())) {
            return ResponseEntity.badRequest().build(); // Round has already ended
        }

        Game game = gameOptional.get();
        Player player = (Player) session.getAttribute(game.getRoomId());
        if (player == null || !player.getPlayerId().equals(playerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Answer answer = new Answer();
        answer.setAnswerId(UUID.randomUUID().toString());
        answer.setQuestionId(questionId);
        answer.setPlayerId(playerId);
        answer.setAnswer((String) body.get("answer"));

        // Check whether the submited answer is correct
        Question question = questionOptional.get();
        answer.setCorrect(false);
        for (String correctAnswer : question.getCorrectAnswers()) {
            if (answer.getAnswer().equalsIgnoreCase(correctAnswer)) {
                answer.setCorrect(true);
            }
        }

        answerDAO.save(answer);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/games/{gameId}/rounds/{roundId}/questions/{questionId}/players/{playerId}")
    public ResponseEntity<Answer> getAnswer(
            @PathVariable String gameId,
            @PathVariable String roundId,
            @PathVariable String questionId,
            @PathVariable String playerId,
            HttpSession session) throws SQLException {
        Optional<Game> gameOptional = gameDAO.findById(gameId);
        Optional<Player> playerOptional = playerDAO.findById(playerId);
        Optional<Question> questionOptional = questionDAO.findById(questionId);
        Optional<Round> roundOptional = roundDAO.findById(roundId);
        Optional<Answer> answerOptional = answerDAO.findByQuestionIdAndPlayerId(questionId, playerId);
        if (gameOptional.isEmpty()
                || playerOptional.isEmpty()
                || questionOptional.isEmpty()
                || roundOptional.isEmpty()
                || answerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Round round = roundOptional.get();
        if (Instant.now().isBefore(round.getEndedAt())) {
            return ResponseEntity.badRequest().build(); // Round has not ended yet
        }

        Game game = gameOptional.get();
        Player player = (Player) session.getAttribute(game.getRoomId());
        if (player == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Answer answer = answerOptional.get();
        return ResponseEntity.ok(answer);
    }
}
