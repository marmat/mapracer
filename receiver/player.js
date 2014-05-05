
var static_playerNumber = 1;



Player = function(id, game) {
  this.id = id;
  this.game = game;
  this.name = 'Player ' + static_playerNumber++;
  this.state = null;

  /** Determine the player's individual color */
  this.hue = Math.round(Math.random() * 360);
  this.colorDark = 'hsl(' + this.hue + ', 80%, 25%)';
  this.colorRegular = 'hsl(' + this.hue + ', 80%, 45%)';
  this.colorLight = 'hsl(' + this.hue + ', 80%, 65%)';

  this.marker = new google.maps.Marker({
    map: this.game.map,
    position: {lat: 0, lng: 0},
    icon: {
      path: google.maps.SymbolPath.CIRCLE,
      fillColor: this.colorRegular,
      fillOpacity: 1,
      scale: 8,
      strokeColor: this.colorDark,
      strokeOpacity: 1,
      strokeWeight: 2
    }
  });

  this.path = new google.maps.Polyline({
    map: this.game.map,
    path: [new google.maps.LatLng(0, 0)],
    strokeColor: this.colorRegular,
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
      var icon = this.marker.getIcon();
      icon.fillColor = this.colorRegular;
      icon.strokeWeight = 2;
      icon.strokeColor = this.colorDark;
      this.marker.setIcon(icon);
      break;
    case PlayerState.FINISHED:
      this.time = Date.now() - this.game.race[DATA_START_TIME];
      var icon = this.marker.getIcon();
      icon.fillColor = this.colorLight;
      icon.strokeWeight = 4;
      icon.strokeColor = this.colorRegular;
      this.marker.setIcon(icon);
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
