CREATE TABLE item_consume (
    id INT UNSIGNED NOT NULL,
    game_id INT UNSIGNED NOT NULL,
    time_consumed TIMESTAMP NOT NULL,
    item VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (game_id) REFERENCES game (id)
);