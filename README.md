# trivia-api

A REST API for a trivia game.

## Running

```bash
docker compose up
```

## Functional requirements

- R01. A user can create a room. Each room has a unique, random and human-friendly URL that can be shared with other users.
- R02. A user can join a room, given the URL of that room. The user must specify a username when joining a room.
- R03. A user becomes the host of a room if it’s the first user that joins the room, or if the former host(s) left the room.
- R04. The host of a room can change the game’s settings (number of rounds, time limit per round, questions per round, teams, max players per team, difficulty…).
- R05. The host of a room can start a game with the current users in that room. Users can pick a team with empty slots. The host can put any user into any team.
- R06. When a round of a game starts, all users can see the question(s) of that round. A question can be either: Multiple Choice (N options to chose from), Short Answer (open-ended reply) or Buzzer (first to answer correctly wins, you lose your turn if answering incorrectly). A question can have both text and media (audio or video). Each correct answer awards points to the team.
- R07. A user can reply to a question until the round timer runs out, and they must not be able to know whether the reply is correct until the end of the round.
- R08. After a round ends, a user can see the correct answers to the questions asked that round and whether their reply was correct.
- R09. A user can see the scoreboard with the current points earned by each team.
- R10. After the game ends, a user can see the final scoreboard and the winners.
