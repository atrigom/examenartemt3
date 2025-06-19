package com.example.trivia.controller;

import com.example.trivia.DAO.PlayerDAO;
import com.example.trivia.DAO.RoomDAO;
import com.example.trivia.DAO.SettingsDAO;
import com.example.trivia.DAO.TeamDAO;
import com.example.trivia.model.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@RestController
public class RoomController {
    private final RoomDAO roomDAO;
    private final PlayerDAO playerDAO;
    private final TeamDAO teamDAO;
    private final SettingsDAO settingsDAO;

    public RoomController(
            RoomDAO roomDAO,
            PlayerDAO playerDAO,
            TeamDAO teamDAO,
            SettingsDAO settingsDAO) {
        this.roomDAO = roomDAO;
        this.playerDAO = playerDAO;
        this.teamDAO = teamDAO;
        this.settingsDAO = settingsDAO;
    }

    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom() throws SQLException {
        Room room = new Room();
        room.setRoomId(UUID.randomUUID().toString());
        room.setCreatedAt(Instant.now());
        roomDAO.save(room);

        // Set default settings for the room
        Settings settings = new Settings();
        settings.setSettingsId(UUID.randomUUID().toString());
        settings.setRounds(10);
        settings.setTimePerRound(60);
        settings.setQuestionsPerRound(5);
        settings.setDifficulty("easy");
        settings.setMaxPlayersPerTeam(5);
        settingsDAO.save(settings);

        URI roomUrl = URI.create("/rooms/" + room.getRoomId());
        return ResponseEntity.created(roomUrl).body(room);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Room> getRoom(@PathVariable String roomId) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Room room = roomOptional.get();
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId,
            HttpSession session) throws SQLException {
        Player player = (Player) session.getAttribute(roomId);
        if (player == null || !player.isHost()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        roomDAO.deleteById(roomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{roomId}/players")
    public ResponseEntity<Player> joinRoom(@PathVariable String roomId,
            @RequestBody Map<String, Object> body,
            HttpSession session) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = new Player();
        player.setPlayerId(UUID.randomUUID().toString());
        player.setRoomId(roomId);
        player.setUsername((String) body.get("username"));
        player.setHost(player.findByRoomId(roomId).isEmpty()); // first player to join is host
        playerDAO.save(player);

        session.setAttribute(roomId, player);
        URI location = URI.create("/rooms/" + roomId + "/players/" + player.getPlayerId());
        return ResponseEntity.created(location).body(player);
    }

    @GetMapping("/rooms/{roomId}/players")
    public ResponseEntity<List<Player>> getRoomPlayers(@PathVariable String roomId) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Player> players = playerDAO.findByRoomId(roomId);
        return ResponseEntity.ok(players);
    }

    @DeleteMapping("/rooms/{roomId}/players/{playerId}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String roomId,
            @PathVariable String playerId,
            HttpSession session) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = (Player) session.getAttribute(roomId);
        if (player == null || !player.isHost() && !player.getPlayerId().equals(playerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        playerDAO.deleteById(playerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{roomId}/teams")
    public ResponseEntity<Team> createTeam(@PathVariable String roomId, HttpSession session) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = (Player) session.getAttribute(roomId);
        if (player == null || !player.isHost()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Team team = new Team();
        team.setTeamId(UUID.randomUUID().toString());
        team.setRoomId(roomId);
        teamDAO.save(team);

        URI location = URI.create("/rooms/" + roomId + "/teams/" + team.getTeamId());
        return ResponseEntity.created(location).body(team);
    }

    @GetMapping("/rooms/{roomId}/teams")
    public ResponseEntity<List<Team>> getTeams(@PathVariable String roomId) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Team> teams = teamDAO.findByRoomId(roomId);
        return ResponseEntity.ok(teams);
    }

    @DeleteMapping("/rooms/{roomId}/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String roomId,
            @PathVariable String teamId,
            HttpSession session) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = (Player) session.getAttribute(roomId);
        if (player == null || !player.isHost()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        teamDAO.deleteById(teamId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms/{roomId}/teams/{teamId}/players")
    public ResponseEntity<List<Player>> getTeamPlayers(@PathVariable String roomId,
            @PathVariable String teamId) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        Optional<Team> teamOptional = teamDAO.findById(teamId);
        if (roomOptional.isEmpty() || teamOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Player> players = playerDAO.findByTeamId(teamId);
        return ResponseEntity.ok(players);
    }

    @PutMapping("/rooms/{roomId}/teams/{teamId}/players/{playerId}")
    public ResponseEntity<?> assignPlayerToTeam(@PathVariable String roomId,
            @PathVariable String teamId,
            @PathVariable String playerId) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        Optional<Team> teamOptional = teamDAO.findById(teamId);
        Optional<Player> playerOptional = playerDAO.findById(playerId);
        if (roomOptional.isEmpty()
                || teamOptional.isEmpty()
                || playerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Team assignment validation
        Room room = roomOptional.get();
        Optional<Settings> settingsOptional = settingsDAO.findById(room.getSettingsId());
        if (settingsOptional.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Player> players = playerDAO.findByTeamId(teamId);
        Settings settings = settingsOptional.get();
        if (players.size() >= settings.getMaxPlayersPerTeam()) {
            return ResponseEntity.badRequest().build();
        }

        Player player = playerOptional.get();
        player.setTeamId(teamId);
        playerDAO.save(player);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/rooms/{roomId}/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> removePlayerFromTeam(@PathVariable String roomId,
            @PathVariable String teamId,
            @PathVariable String playerId,
            HttpSession session) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        Optional<Team> teamOptional = teamDAO.findById(teamId);
        Optional<Player> playerOptional = playerDAO.findById(playerId);
        if (roomOptional.isEmpty()
                || teamOptional.isEmpty()
                || playerOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player currentPlayer = (Player) session.getAttribute(roomId);
        if (currentPlayer == null
                || !currentPlayer.isHost() && !currentPlayer.getPlayerId().equals(playerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Player player = playerOptional.get();
        player.setTeamId(null);
        playerDAO.save(player);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms/{roomId}/settings")
    public ResponseEntity<Settings> getSettings(@PathVariable String roomId) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Room room = roomOptional.get();
        Optional<Settings> settingsOptional = settingsDAO.findById(room.getSettingsId());
        if (settingsOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Settings settings = settingsOptional.get();
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/rooms/{roomId}/settings")
    public ResponseEntity<Settings> updateSettings(@PathVariable String roomId,
            @RequestBody Settings settings,
            HttpSession session) throws SQLException {
        Optional<Room> roomOptional = roomDAO.findById(roomId);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Player player = (Player) session.getAttribute(roomId);
        if (player == null || !player.isHost()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        settingsDAO.save(settings);
        return ResponseEntity.ok(settings);
    }
}
