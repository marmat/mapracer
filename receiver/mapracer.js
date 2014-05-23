/**
 * MapRacer Receiver Code
 * @author kaktus621@gmail.com (Martin Matysiak)
 */



/** @constructor */
MapRacer = function() {

  /** @type {Element} */
  this.mapEl = document.querySelector('#map');

  /** @type {Element} */
  this.timeEl = document.querySelector('#time');

  /** @type {Element} */
  this.titleEl = document.querySelector('#title');

  /** @type {Element} */
  this.infoEl = document.querySelector('#info');

  /** @type {Element} */
  this.targetEl = document.querySelector('#target');

  /** @type {Element} */
  this.splashEl = document.querySelector('#splash');

  /** @type {Countdown} */
  this.countdown = new Countdown(document.querySelector('#countdown'));
  this.countdown.addFinishCallback(this.setState.bind(this, GameState.RACE));

  /** @type {Leaderboard} */
  this.leaderboard = new Leaderboard(document.querySelector('#leaderboard'));

  /** @type {cast.receiver.CastReceiverManager} */
  this.receiverManager = null;

  /** @type {cast.receiver.CastMessageBus} */
  this.messageBus = null;

  /** @type {google.maps.Map} */
  this.map = null;

  /** @type {google.maps.StreetViewService} */
  this.streetViewService = null;

  /** @type {google.maps.Icon} */
  this.targetIcon = {
    url: 'target.png',
    size: new google.maps.Size(48, 48),
    anchor: new google.maps.Point(9, 43)
  };

  /** @type {Race} */
  this.race = new Race();

  /** @type {GameState} */
  this.state = GameState.INIT;

  /**
   * Map from Player ID to the Player object.
   * @type {Object.<string, Player>}
   */
  this.players = {};

  /**
   * Map from Sender ID to Player ID (many-to-one relationship).
   * @type {Obejct.<string, string>}
   */
  this.senders = {};

  this.initializeCast_();
  this.initializeMap_();
};


/** @private */
MapRacer.prototype.initializeCast_ = function() {
  this.receiverManager = cast.receiver.CastReceiverManager.getInstance();
  this.receiverManager.onSenderConnected = this.onConnect.bind(this);
  this.receiverManager.onSenderDisconnected = this.onDisconnect.bind(this);

  this.messageBus = this.receiverManager.getCastMessageBus(NAMESPACE,
      cast.receiver.CastMessageBus.MessageType.JSON);
  this.messageBus.onMessage = this.onCastMessage.bind(this);

  this.receiverManager.start();
};


/** @private */
MapRacer.prototype.initializeMap_ = function() {
  // We disable all the controls as the user can't control anything anyway
  // directly on the TV.
  var mapOptions = {
    mapTypeId: google.maps.MapTypeId.ROADMAP,
    disableDefaultUI: true,
    draggable: false
  };

  this.map = new google.maps.Map(this.mapEl, mapOptions);
  this.streetViewService = new google.maps.StreetViewService();
};


/** @private */
MapRacer.prototype.broadcastState_ = function() {
  this.messageBus.broadcast({
    type: MessageType.GAME_STATE,
    players: this.getPlayerCount(),
    race: this.race.serialize(),
    state: this.state
  });
};


/** @return {number} The number of players that are alive. */
MapRacer.prototype.getPlayerCount = function() {
  var isAlive = function(playerId) {
    return !this.players[playerId].isSuspended();
  };

  return Object.keys(this.players).filter(isAlive, this).length;
};


/** @param {GameState} state The new state to show. */
MapRacer.prototype.setState = function(state) {
  this.state = state;
  switch (state) {
    case GameState.INIT:
      this.race = new Race();
      this.splashEl.style.opacity = 1;
      this.infoEl.innerHTML = '';
      this.leaderboard.setFullscreen(false);
      // Reset all connected clients
      for (var id in this.players) {
        this.players[id].setState(PlayerState.WAITING);
      }
      break;
    case GameState.LOAD:
      this.infoEl.innerHTML = 'Get Ready!';
      this.countdown.start(COUNTDOWN_DURATION);
      break;
    case GameState.RACE:
      this.splashEl.style.opacity = '0';
      this.race.startTime = Date.now();
      this.timerInterval_ = setInterval(this.updateTimer.bind(this), 10);
      break;
    case GameState.SCORES:
      clearInterval(this.timerInterval_);
      this.leaderboard.setFullscreen(true);
      setTimeout(this.setState.bind(this, GameState.INIT),
          SCORE_DURATION * S_TO_MS);
      break;
  }

  this.broadcastState_();
};


/***/
MapRacer.prototype.maybeStartRace = function() {
  if (!this.race.startLocation ||
      !this.race.targetLocation ||
      this.getPlayerCount() < MIN_PLAYERS) {
    // we are not ready yet
    return;
  }

  if (this.state != GameState.INIT) {
    // we have already started
    return;
  }

  this.targetEl.innerHTML = this.race.title;
  this.map.setCenter(this.race.startLocation);

  var raceBounds = new google.maps.LatLngBounds();
  raceBounds.extend(this.race.startLocation);
  raceBounds.extend(this.race.targetLocation);
  this.map.fitBounds(raceBounds);

  new google.maps.Marker({
    map: this.map,
    position: this.race.targetLocation,
    icon: this.targetIcon
  });

  // Reset all players
  for (var playerId in this.players) {
    var player = this.players[playerId];
    player.setStartPosition(this.race.startLocation);
    player.setState(PlayerState.ACTIVE);
  }

  this.setState(GameState.LOAD);
};


/***/
MapRacer.prototype.maybeFinishRace = function() {
  var isActive = function(id) {
    return this.players[id].isActive();
  };

  if (!Object.keys(this.players).some(isActive.bind(this))) {
    // TODO: also stop if at least one has finished and some timeout has passed
    this.setState(GameState.SCORES);
  }
};


/** Updates the visible UI timer */
MapRacer.prototype.updateTimer = function() {
  var difference = Date.now() - this.race.startTime;
  this.timeEl.innerHTML = formatTime(difference);
};


/**
 * @param {google.maps.LatLng=} opt_bias The user's coarse location in order to
 *     choose a POI in his proximity (if available).
 * @return {google.maps.LatLng} A random POI that can be used as a race target.
 */
MapRacer.prototype.pickTarget = function(opt_bias) {
  return new google.maps.LatLng(37.420283, -122.083961);
};


/**
 * @param {google.maps.LatLng} target The chosen target location.
 * @return {google.maps.LatLng} A location somewhat close to the given target
 *    which can be used as a starting location for the race.
 */
MapRacer.prototype.pickStart = function(target) {
  return new google.maps.LatLng(37.413084, -122.069217);
};


/**
 * Callback for the StreetViewService.
 * @param {string} id The type of location that was requested.
 * @param {StreetViewPanoramaData} panorama The closest panorama, if any.
 * @param {StreetViewStatus} status The status returned by StreetViewService.
 */
MapRacer.prototype.onStreetViewLocation = function(id, panorama, status) {
  if (status == google.maps.StreetViewStatus.OK) {
    this.race[id] = panorama.location.latLng;
  } else {
    this.infoEl.innerHTML = 'Warning: StreetView not available in the ' +
        'desired game area. Please try a different location.';
  }

  this.maybeStartRace();
};


/** @param {cast.receiver.CastMessageBus.Event} message The incoming message. */
MapRacer.prototype.onCastMessage = function(message) {
  console.dir(message);

  var data = message.data;
  var playerId = this.senders[message.senderId];
  switch (data.type) {
    case MessageType.REQUEST:
      this.onGameRequest(playerId, data);
      break;
    case MessageType.POSITION:
      if (playerId in this.players) {
        var location = new google.maps.LatLng(
            data.location.lat, data.location.lng);
        this.players[playerId].onPosition(location);
      }
      break;
    case MessageType.LOGIN:
      this.onLogin(message.senderId, data);
      break;
    case MessageType.LOGOUT:
      this.onLogout(message.senderId, data);
      break;
  }
};


/**
 * @param {string} playerId The player's ID.
 * @param {Object} payload The message payload.
 */
MapRacer.prototype.onGameRequest = function(playerId, payload) {
  if (this.state != GameState.INIT) {
    return;
  }

  // Parse the incoming request and initialize a new race object
  var request = new Race(payload);
  this.race = new Race();
  this.race.title = request.title;

  // Check back with StreetView if the provided locations are valid
  var targetLocation = request.targetLocation || this.pickTarget();
  var startLocation = request.startLocation || this.pickStart(targetLocation);

  this.streetViewService.getPanoramaByLocation(targetLocation, 50,
      this.onStreetViewLocation.bind(this, 'targetLocation'));

  this.streetViewService.getPanoramaByLocation(startLocation, 50,
      this.onStreetViewLocation.bind(this, 'startLocation'));
};


/**
 * After connecting, the sender has to specify its ID and possibly name.
 * Using the senderId does not work with Android devices as the IDs change
 * when switching views.
 * @param {string} senderId The sender's cast ID.
 * @param {Object} payload The login object.
 */
MapRacer.prototype.onLogin = function(senderId, payload) {
  console.log('Client login: ' + senderId);

  // Check if a sender has just reconnected or if it's an entirely new one
  if (payload.id in this.players) {
    this.players[payload.id].resume();
    this.players[payload.id].setSenderId(senderId);
  } else {
    this.players[payload.id] = new Player(payload.id, this,
        payload.name, senderId);
  }

  this.senders[senderId] = payload.id;
  this.broadcastState_();
  this.maybeStartRace();
};


/**
 * A logout message indicates that the player has no intention of coming back
 * i.e. he isn't disconnecting because of switching views or something like
 * that. This allows us to completely remove the player.
 * @param {string} senderId The sender's cast ID.
 * @param {Object} payload The logout object.
 */
MapRacer.prototype.onLogout = function(senderId, payload) {
  if (!(senderId in this.senders)) {
    return;
  }

  var playerId = this.senders[senderId];
  if (!(playerId in this.players)) {
    return;
  }

  this.players[playerId].dispose();
  delete this.players[playerId];
  this.broadcastState_();
  this.maybeFinishRace();
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onConnect = function(client) {
  console.log('Client connect: ' + client.data);
  this.senders[client.data] = null;
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onDisconnect = function(client) {
  console.log('Client disconnect: ' + client.data);
  if (!(client.data in this.senders)) {
    return;
  }

  var playerId = this.senders[client.data];
  if (!(playerId in this.players)) {
    return;
  }

  // Note: when switching views in Android, the client first disconnects and
  // then reconnects. If we delete the player after disconnect, we lose him
  // (bad!). Instead, we will have to "suspend" the user (make him invisible for
  // all intents and purposes), but keep it in the player object nonetheless.

  this.players[playerId].suspend();
  this.broadcastState_();
  this.maybeFinishRace();
};
