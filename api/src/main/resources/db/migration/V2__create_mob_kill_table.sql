CREATE TABLE mob_kill
(
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  game_id     INT UNSIGNED NOT NULL,
  player_id   VARCHAR(36)  NOT NULL,
  mob         varchar(32)  NOT NULL,
  time_killed TIMESTAMP    NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);