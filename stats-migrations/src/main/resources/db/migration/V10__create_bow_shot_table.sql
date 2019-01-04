CREATE TABLE bow_shot (
    id INT UNSIGNED NOT NULL,
    game_id INT UNSIGNED NOT NULL,
    player_id VARCHAR(36) NOT NULL,
    hit BOOLEAN NOT NULL,
    distance DOUBLE NOT NULL,
    time_shot TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
)