CREATE TABLE location
(
  id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
  game_id   INT UNSIGNED NOT NULL,
  player_id VARCHAR(36)  NOT NULL,
  time_created TIMESTAMP NOT NULL,
  x         INT          NOT NULL,
  y         INT          NOT NULL,
  z         INT          NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);