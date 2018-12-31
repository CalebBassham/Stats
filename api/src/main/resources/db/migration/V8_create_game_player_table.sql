CREATE TABLE game_player (
    id INT UNSIGNED NOT NULL,
    game_id INT UNSIGNED NOT NULL,
    player_id VARCHAR(36) NOT NULL,
    time_started TIMESTAMP NOT NULL,
    items_enchanted INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY(id)
)