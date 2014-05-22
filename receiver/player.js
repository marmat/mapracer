
var static_playerNumber = 1;



/**
 * @param {string} id The player's ID.
 * @param {MapRacer} game The game instance.
 * @param {string=} opt_name The player's name.
 * @param {string=} opt_senderId The player's last known senderId.
 * @constructor
 */
Player = function(id, game, opt_name, opt_senderId) {
  this.id = id;
  this.game = game;
  this.name = opt_name || ('Player ' + static_playerNumber++);
  this.state = null;
  this.senderId = opt_senderId || null;
  this.suspendedState_ = null;

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


/** Destructor */
Player.prototype.dispose = function() {
  this.game.leaderboard.remove(this.id);
  this.marker.setMap(null);
  this.path.setMap(null);
};


/** States may be set, but otherwise this player won't be visible externally. */
Player.prototype.suspend = function() {
  if (!this.suspendedState_) {
    var lastState = this.state;
    this.setState(PlayerState.WAITING);
    this.suspendedState_ = lastState;
  }
};


/** Inverse operation to suspend. */
Player.prototype.resume = function() {
  if (!!this.suspendedState_) {
    var lastState = this.suspendedState_;
    this.suspendedState_ = null;
    this.setState(lastState);
  }
};


/** @return {boolean} Whether the player is currently suspended. */
Player.prototype.isSuspended = function() {
  return !!this.suspendedState_;
};


/** @return {boolean} Whether the player is currently active. */
Player.prototype.isActive = function() {
  return this.state == PlayerState.ACTIVE ||
      (this.suspendedState_ && this.suspendedState_ == PlayerState.ACTIVE);
};


/** @param {google.maps.LatLng} position The player's new position. */
Player.prototype.onPosition = function(position) {
  if (this.state == PlayerState.ACTIVE) {
    this.marker.setPosition(position);
    this.path.getPath().push(position);

    var distanceToFinish =
        google.maps.geometry.spherical.computeDistanceBetween(
        position, this.game.race.targetLocation);

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


/** @param {string} senderId The player's new or last known sender ID. */
Player.prototype.setSenderId = function(senderId) {
  this.senderId = senderId;

  // Resend the player's state to make sure it has the right data
  this.game.messageBus.send(this.senderId, {
    type: MessageType.PLAYER_STATE,
    state: this.state
  });
};


/** @param {PlayerState} state The player's new state. */
Player.prototype.setState = function(state) {
  // Do nothing if we aren't changing the state
  if (this.state == state) {
    return;
  }

  // If we are currently suspended, just remember the new state, but do not
  // act upon it.
  if (!!this.suspendedState_) {
    this.suspendedState_ = state;
    return;
  }

  this.state = state;
  switch (state) {
    case PlayerState.WAITING:
      this.game.leaderboard.remove(this.id);
      this.marker.setVisible(false);
      this.path.setVisible(false);
      break;
    case PlayerState.ACTIVE:
      this.game.leaderboard.add(this.id, this.name, Infinity, this.colorLight);
      this.marker.setVisible(true);
      this.path.setVisible(true);
      var icon = this.marker.getIcon();
      icon.fillColor = this.colorRegular;
      icon.strokeWeight = 2;
      icon.strokeColor = this.colorDark;
      this.marker.setIcon(icon);
      break;
    case PlayerState.FINISHED:
      this.time = Date.now() - this.game.race.startTime;
      var icon = this.marker.getIcon();
      icon.fillColor = this.colorLight;
      icon.strokeWeight = 4;
      icon.strokeColor = this.colorRegular;
      this.marker.setIcon(icon);
      this.game.leaderboard.update(this.id, -1 / this.time,
          this.name + ' (' + formatTime(this.time) + ')');
      this.game.maybeFinishRace();
      break;
  }

  this.game.messageBus.send(this.senderId, {
    type: MessageType.PLAYER_STATE,
    state: this.state
  });
};
