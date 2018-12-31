CREATE TABLE fishing_item_caught
(
  id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  game_id     INT UNSIGNED    NOT NULL,
  item_id     BIGINT UNSIGNED NOT NULL,
  time_caught TIMESTAMP       NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE,
  FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
)