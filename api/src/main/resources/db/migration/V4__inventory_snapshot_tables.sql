CREATE TABLE item
(
  id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type   VARCHAR(64)     NOT NULL,
  amount INT             NOT NULL,
  damage INT,
  PRIMARY KEY (id)
);

CREATE TABLE enchantment
(
  id                INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  item_id           BIGINT UNSIGNED NOT NULL,
  enchantment       VARCHAR(64)     NOT NULL,
  enchantment_level TINYINT         NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
);

CREATE TABLE inventory
(
  id           INT UNSIGNED NOT NULL,
  player_id    VARCHAR(36)  NOT NULL,
  game_id      INT UNSIGNED NOT NULL,
  time_created TIMESTAMP    NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

CREATE TABLE inventory_item
(
  id           INT UNSIGNED    NOT NULL,
  inventory_id INT UNSIGNED    NOT NULL,
  item_id      BIGINT UNSIGNED NOT NULL,
  slot         INT UNSIGNED    NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (inventory_id) REFERENCES inventory (id) ON DELETE CASCADE,
  FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE
);