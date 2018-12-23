CREATE TABLE block_broken
(
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  game_id     INT UNSIGNED    NOT NULL,
  player_id   VARCHAR(36)     NOT NULL,
  block       VARCHAR(64)     NOT NULL,
  time_broken TIMESTAMP       NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);