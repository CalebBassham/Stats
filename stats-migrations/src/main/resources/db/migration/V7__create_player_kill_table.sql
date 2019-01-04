CREATE TABLE player_kill (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  game_id INT UNSIGNED NOT NULL,

  killed_player_id VARCHAR(36) NOT NULL,
  killed_player_inventory_id INT UNSIGNED NOT NULL,
  killed_player_location_id INT UNSIGNED NOT NULL,
  killed_player_experience INT NOT NULL,

  killer_player_id VARCHAR(36) NOT NULL,
  killer_player_inventory_id INT UNSIGNED NOT NULL,
  killer_player_location_id INT UNSIGNED NOT NULL,
  killer_health_remaining DOUBLE NOT NULL,
  killer_max_health DOUBLE NOT NULL,

  PRIMARY KEY (id),
  FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE,
  FOREIGN KEY (killed_player_location_id) REFERENCES location (id) ON DELETE CASCADE,
  FOREIGN KEY (killed_player_inventory_id) REFERENCES inventory (id) ON DELETE CASCADE,
  FOREIGN KEY (killer_player_location_id) REFERENCES location (id) ON DELETE CASCADE,
  FOREIGN KEY (killer_player_inventory_id) REFERENCES inventory (id) ON DELETE CASCADE
);