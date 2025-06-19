CREATE TABLE settings (
    settings_id SERIAL PRIMARY KEY,
    rounds INT NOT NULL,
    time_per_round INT NOT NULL,
    questions_per_round INT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    max_players_per_team INT NOT NULL
);

CREATE TABLE rooms (
    room_id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    settings_id INT,
    FOREIGN KEY (settings_id) REFERENCES settings(settings_id)
);

CREATE TABLE teams (
    team_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id)
);

CREATE TABLE players (
    player_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    host BOOLEAN NOT NULL,
    team_id INT,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    FOREIGN KEY (team_id) REFERENCES teams(team_id)
);

CREATE TABLE games (
    game_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    settings_id INT,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    FOREIGN KEY (settings_id) REFERENCES settings(settings_id)
);

CREATE TABLE rounds (
    round_id SERIAL PRIMARY KEY,
    game_id INT NOT NULL,
    round_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(game_id)
);

CREATE TABLE questions (
    question_id SERIAL PRIMARY KEY,
    round_id INT NOT NULL,
    type VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    media_url TEXT,
    options TEXT[],
    correct_answers TEXT[] NOT NULL,
    points INT NOT NULL,
    FOREIGN KEY (round_id) REFERENCES rounds(round_id)
);

CREATE TABLE answers (
    answer_id SERIAL PRIMARY KEY,
    question_id INT NOT NULL,
    player_id INT NOT NULL,
    answer TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    FOREIGN KEY (question_id) REFERENCES questions(question_id),
    FOREIGN KEY (player_id) REFERENCES players(player_id)
);
