CREATE TABLE mob_tamed (
    id INT UNSIGNED NOT NULL,
    game_id INT UNSIGNED NOT NULL,
    player_id VARCHAR(36) NOT NULL,
    mob VARCHAR(32) NOT NULL,
    time_tamed TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);