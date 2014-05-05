
var static_playerNumber = 1;



Player = function(id, game) {
  this.id = id;
  this.game = game;
  this.name = 'Player ' + static_playerNumber++;
  this.state = null;

  this.marker = new google.maps.Marker({
    map: this.game.map,
    position: {lat: 0, lng: 0},
    icon: this.game.playerIcon
  });

  this.path = new google.maps.Polyline({
    map: this.game.map,
    path: [new google.maps.LatLng(0, 0)],
    strokeColor: '#4390F7',
    strokeOpacity: 0.6,
    strokeWeight: 4
  });

  this.setState(PlayerState.WAITING);
};


/** @return {boolean} Whether the player is currently active. */
Player.prototype.isActive = function() {
  return this.state == PlayerState.ACTIVE;
};


/** @param {google.maps.LatLng} position The player's new position. */
Player.prototype.onPosition = function(position) {
  if (this.state == PlayerState.ACTIVE) {
    this.marker.setPosition(position);
    this.path.getPath().push(position);

    var distanceToFinish =
        google.maps.geometry.spherical.computeDistanceBetween(
        position, this.game.race[DATA_TARGET_LOCATION]);

    this.game.leaderboard.update(this.id, distanceToFinish);
    if (distanceToFinish < WIN_DISTANCE_THRESHOLD) {
      console.log('Player has finished! (' + this.id + ')');
      this.setState(PlayerState.FINISHED);
    }
  }
};


/** @param {google.maps.LatLng} position The player's initial position. */
Player.prototype.setStartPosition = function(position) {
  this.marker.setPosition(position);
  this.path.setPath([position]);
};


/** @param {PlayerState} state The player's new state. */
Player.prototype.setState = function(state) {
  this.state = state;
  switch (state) {
    case PlayerState.WAITING:
      this.game.leaderboard.remove(this.id);
      this.marker.setVisible(false);
      break;
    case PlayerState.ACTIVE:
      this.game.leaderboard.add(this.id, this.name, Infinity);
      this.marker.setVisible(true);
      break;
    case PlayerState.FINISHED:
      this.time = Date.now() - this.game.race[DATA_START_TIME];
      this.marker.setIcon(this.game.playerFinishedIcon);
      this.game.leaderboard.update(this.id, -1 / this.time);
      this.game.maybeFinishRace();
      break;
  }

  this.game.messageBus.send(this.id, {
    type: MessageType.STATUS,
    game: this.game.state,
    state: this.state
  });
};
