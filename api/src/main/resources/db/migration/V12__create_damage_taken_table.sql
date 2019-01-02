CREATE TABLE damage_taken (
    id INT UNSIGNED NOT NULL,
    game_id INT UNSIGNED NOT NULL,
    time_taken TIMESTAMP NOT NULL,

    damage_cause VARCHAR(64) NOT NULL,
    damage_taken DOUBLE NOT NULL,

    damaged_player_health DOUBLE NOT NULL,
    damaged_player_max_health DOUBLE NOT NULL,
    damaged_player_id VARCHAR(36) NOT NULL,

    damaging_mob VARCHAR(32),
    damaging_mob_health DOUBLE,
    damaging_mob_max_health DOUBLE,
    damaging_player_id VARCHAR(36),

    PRIMARY KEY (id),
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);