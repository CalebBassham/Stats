CREATE TABLE mob_kill
(
  id        INT         NOT NULL AUTO_INCREMENT,
  game_id   INT         NOT NULL,
  player_id VARCHAR(36) NOT NULL,
  mob       varchar(32) NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX game_id_index ON mob_kill (game_id);
CREATE INDEX player_id_index ON mob_kill (player_id);