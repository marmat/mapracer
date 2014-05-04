/**
 * MapRacer Receiver Code
 * @author kaktus621@gmail.com (Martin Matysiak)
 */


/** @const */
var NAMESPACE = 'urn:x-cast:de.martinmatysiak.mapracer';
var MIN_PLAYERS = 1;
var COUNTDOWN_DURATION = 5;
var WIN_DISTANCE_THRESHOLD = 50; // in meters

var DATA_TYPE = 'type';
var DATA_ACTIVE = 'active';
var DATA_START_TIME = 'start_time';
var DATA_START_LOCATION = 'start_location';
var DATA_TARGET_LOCATION = 'target_location';
var DATA_TARGET_TITLE = 'target_title';

var GameState = {
  INIT: 'init',
  LOAD: 'load',
  RACE: 'race',
  SCORES: 'scores'
};

var MessageType = {
  REQUEST: 'request',
  START: 'start',
  POSITION: 'position',
  STOP: 'stop',
  PLAYER_COUNT: 'player_count'
};

var PlayerStatus = {
  READY: 'ready',
  ACTIVE: 'active',
  WAITING: 'waiting',
  FINISHED: 'finished'
};



/** @constructor */
MapRacer = function() {

  /** @type {Element} */
  this.mapEl = document.querySelector('#map');

  /** @type {Element} */
  this.timeEl = document.querySelector('#time');

  /** @type {Element} */
  this.titleEl = document.querySelector('#title');

  /** @type {Element} */
  this.targetEl = document.querySelector('#target');

  /** @type {Element} */
  this.splashEl = document.querySelector('#splash');

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

  /** @type {google.maps.Icon} */
  this.playerIcon = {
    url: 'player.png',
    size: new google.maps.Size(24, 24),
    anchor: new google.maps.Point(12, 12)
  };

  /** @type {google.maps.Icon} */
  this.playerFinishedIcon = {
    url: 'playerFinished.png',
    size: new google.maps.Size(24, 24),
    anchor: new google.maps.Point(12, 12)
  };

  /** @type {Object.<string, *>} */
  this.race = null;

  /** @type {GameState} */
  this.state = GameState.INIT;

  /**
   * Map from Sender ID to the Sender object containing all connected devices.
   * @type {Object.<string, cast.receiver.system.Sender>}
   */
  this.players = {};

  this.initializeCast_();
  this.initializeMap_();
};


/** @private */
MapRacer.prototype.initializeCast_ = function() {
  this.receiverManager = cast.receiver.CastReceiverManager.getInstance();
  this.receiverManager.onSenderConnected = this.onConnect.bind(this);
  this.receiverManager.onSenderDisconnected = this.onDisconnect.bind(this);

  this.messageBus = this.receiverManager.getCastMessageBus(NAMESPACE);
  this.messageBus.onMessage = this.onCastMessage.bind(this);

  this.receiverManager.start();
};


/** @private */
MapRacer.prototype.initializeMap_ = function() {
  // We disable all the controls as the user can't control anything anyway
  // directly on the TV.
  var mapOptions = {
    zoom: 14,
    center: new google.maps.LatLng(50.7658, 6.1059),
    mapTypeId: google.maps.MapTypeId.ROADMAP,
    disableDefaultUI: true,
    draggable: false
  };

  this.map = new google.maps.Map(this.mapEl, mapOptions);
  this.streetViewService = new google.maps.StreetViewService();
};


/** @param {GameState} state The new state to show. */
MapRacer.prototype.setUiState = function(state) {
  this.state = state;
  switch (state) {
    case GameState.INIT:
      this.splashEl.style.opacity = 1;
      this.titleEl.style.display = 'inline';
      this.titleEl.innerHTML = 'MapRacer';
      break;
    case GameState.LOAD:
      this.countdownInterval_ = setInterval(this.countdown_.bind(this), 1000);
      this.titleEl.innerHTML = 'Get Ready!';
      break;
    case GameState.RACE:
      clearInterval(this.countdownInterval_);
      this.splashEl.style.opacity = '0';
      this.race[DATA_START_TIME] = Date.now();
      this.race[DATA_ACTIVE] = true;
      this.timerInterval_ = setInterval(this.updateTimer.bind(this), 10);
      break;
    case GameState.SCORES:
      clearInterval(this.timerInterval_);
      break;
  }
};


/** @private */
MapRacer.prototype.maybeStartRace_ = function() {
  if (!this.race ||
      !this.race[DATA_START_LOCATION] ||
      !this.race[DATA_TARGET_LOCATION] ||
      Object.keys(this.players).length < MIN_PLAYERS) {
    // we are not ready yet
    return;
  }

  if (this.state != GameState.INIT) {
    // we have already started
    return;
  }

  this.targetEl.innerHTML = this.race[DATA_TARGET_TITLE];
  this.map.setCenter(this.race[DATA_START_LOCATION]);

  var raceBounds = new google.maps.LatLngBounds();
  raceBounds.extend(this.race[DATA_START_LOCATION]);
  raceBounds.extend(this.race[DATA_TARGET_LOCATION]);
  this.map.fitBounds(raceBounds);

  new google.maps.Marker({
    map: this.map,
    position: this.race[DATA_TARGET_LOCATION],
    icon: this.targetIcon
  });

  // Reset all players
  for (var playerId in this.players) {
    var player = this.players[playerId];
    player.marker.setPosition(this.race[DATA_START_LOCATION]);
    player.path.setPath([this.race[DATA_START_LOCATION]]);
    player.status = PlayerStatus.ACTIVE;
  }

  // Broadcast the game start event (TODO move somewhere else)
  var payload = {
    type: MessageType.START
  };

  payload[DATA_TARGET_LOCATION] = this.convertLatLng(
      this.race[DATA_TARGET_LOCATION]);
  payload[DATA_START_LOCATION] = this.convertLatLng(
      this.race[DATA_START_LOCATION]);
  payload[DATA_TARGET_TITLE] = this.race[DATA_TARGET_TITLE];

  this.messageBus.broadcast(JSON.stringify(payload));
  this.setUiState(GameState.LOAD);
};


/**
 * @param {google.maps.LatLng} latLng The LatLng object to convert.
 * @return {Object} A better serializible object.
 */
MapRacer.prototype.convertLatLng = function(latLng) {
  return {lat: latLng.lat(), lng: latLng.lng()};
};


/** @private */
MapRacer.prototype.countdown_ = function() {
  var nextCount = COUNTDOWN_DURATION;

  var old = document.querySelector('.counter');
  if (!!old) {
    nextCount = old.innerHTML - 1;
    old.parentNode.removeChild(old);
  }

  if (nextCount > 0) {
    var counter = document.createElement('span');
    counter.innerHTML = nextCount;
    counter.className = 'counter';

    this.splashEl.appendChild(counter);
    setTimeout(function() {
      counter.style.fontSize = '220px';
      counter.style.opacity = '0';
    }, 10);
  } else {
    this.setUiState(GameState.RACE);
  }
};


/** Updates the visible UI timer */
MapRacer.prototype.updateTimer = function() {
  var difference = Date.now() - this.race[DATA_START_TIME];
  var seconds = Math.floor((difference / 1000) % 60);

  var formatted = Math.floor(difference / 60000) + // minutes
      (seconds < 10 ? ':0' : ':') + seconds + // seconds
      '.' + difference % 1000; // milliseconds

  this.timeEl.innerHTML = formatted;
};


/**
 * Checks if the player has won the game etc.
 * @param {String} playerId The player to check.
 * @param {google.maps.LatLng} location The player's new location.
 * @private
 */
MapRacer.prototype.updatePlayerStatus_ = function(playerId, location) {
  var player = this.players[playerId];

  if (!player) {
    console.log('UpdatePlayerStatus: Player not found ' + playerId);
    return;
  }

  if (player.status == PlayerStatus.ACTIVE) {
    player.marker.setPosition(location);
    player.path.getPath().push(location);

    var distanceToFinish =
        google.maps.geometry.spherical.computeDistanceBetween(
        location, this.race[DATA_TARGET_LOCATION]);

    this.leaderboard.update(playerId, distanceToFinish);

    if (distanceToFinish < WIN_DISTANCE_THRESHOLD) {
      console.log('Player has finished! (' + playerId + ')');
      player.status = PlayerStatus.FINISHED;
      player.time = Date.now() - this.race[DATA_START_TIME];
      player.marker.setIcon(this.playerFinishedIcon);
      this.leaderboard.update(playerId, 0);
      // TODO(marmat): Send message to client
      // TODO(marmat): Update leaderboard
    }
  }
};


/** @private */
MapRacer.prototype.sendPlayerCount_ = function() {
  var payload = {
    type: MessageType.PLAYER_COUNT,
    count: Object.keys(this.players).length
  };

  this.messageBus.broadcast(JSON.stringify(payload));
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
    this.titleEl.innerHTML = 'Warning: StreetView not available in the ' +
        'desired game area. Please try a different location.';
  }

  this.maybeStartRace_();
};


/** @param {cast.receiver.CastMessageBus.Event} message The incoming message. */
MapRacer.prototype.onCastMessage = function(message) {
  console.dir(message);

  var payload = JSON.parse(message.data);
  switch (payload[DATA_TYPE]) {
    case MessageType.REQUEST:
      this.onGameRequest(message.senderId, payload);
      break;
    case MessageType.POSITION:
      this.onPosition(message.senderId, payload);
      break;
  }
};


/**
 * @param {String} senderId The player's ID.
 * @param {Object} payload The message payload.
 */
MapRacer.prototype.onGameRequest = function(senderId, payload) {

  this.race = {};
  this.race[DATA_TARGET_TITLE] = payload[DATA_TARGET_TITLE] || 'the finish';

  var targetLocation = null;
  if (!!payload[DATA_TARGET_LOCATION]) {
    targetLocation = new google.maps.LatLng(
        payload[DATA_TARGET_LOCATION].lat,
        payload[DATA_TARGET_LOCATION].lng);
  } else {
    // Pick an interesting location at random
    // TODO
    targetLocation = new google.maps.LatLng(0, 0);
  }

  this.streetViewService.getPanoramaByLocation(targetLocation, 50,
      this.onStreetViewLocation.bind(this, DATA_TARGET_LOCATION));

  var startLocation = null;
  if (!!payload[DATA_START_LOCATION]) {
    startLocation = new google.maps.LatLng(
        payload[DATA_START_LOCATION].lat,
        payload[DATA_START_LOCATION].lng);
  } else {
    // Pick a location somewhat close to the target
    // TODO
    startLocation = new google.maps.LatLng(0, 0);
  }

  this.streetViewService.getPanoramaByLocation(startLocation, 50,
      this.onStreetViewLocation.bind(this, DATA_START_LOCATION));
};


/**
 * @param {String} senderId The player's ID.
 * @param {Object} payload The message payload.
 */
MapRacer.prototype.onPosition = function(senderId, payload) {
  var playerLocation = new google.maps.LatLng(payload.location.lat,
      payload.location.lng);

  this.updatePlayerStatus_(senderId, playerLocation);
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onConnect = function(client) {
  console.log('Client connected!');
  console.dir(client);

  var player = this.receiverManager.getSender(client.data);
  player.marker = new google.maps.Marker({
    map: this.map,
    position: !!this.race ? this.race.start_location : {lat: 0, lng: 0},
    icon: this.playerIcon
  });

  player.path = new google.maps.Polyline({
    map: this.map,
    path: [!!this.race ? this.race.start_location :
          new google.maps.LatLng(0, 0)],
    strokeColor: '#4390F7',
    strokeOpacity: 0.6,
    strokeWeight: 4
  });

  // TODO(marmat): Notify player if race has already started.
  player.status = this.state == GameState.RACE ?
      PlayerStatus.WAITING : PlayerStatus.READY;

  this.players[client.data] = player;
  this.leaderboard.add(client.data);
  this.sendPlayerCount_();
  this.maybeStartRace_();
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onDisconnect = function(client) {
  console.log('Client disconnected!');
  console.dir(client);

  this.leaderboard.remove(client.data);
  this.players[client.data].marker.setMap(null);
  this.players[client.data].path.setMap(null);
  delete this.players[client.data];

  this.sendPlayerCount_();
};
