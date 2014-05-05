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

  /** @type {Object.<string, *>} */
  this.race = null;

  /** @type {GameState} */
  this.state = GameState.INIT;

  /**
   * Map from Sender ID to the Sender object containing all connected devices.
   * @type {Object.<string, Player>}
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


/** @param {GameState} state The new state to show. */
MapRacer.prototype.setState = function(state) {
  this.state = state;
  switch (state) {
    case GameState.INIT:
      this.splashEl.style.opacity = 1;
      this.titleEl.style.display = 'inline';
      this.titleEl.innerHTML = 'MapRacer';
      this.leaderboard.setFullscreen(false);
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
      this.leaderboard.setFullscreen(true);
      setTimeout(this.setState.bind(this, GameState.INIT),
          SCORE_DURATION * S_TO_MS);
      break;
  }
};


/***/
MapRacer.prototype.maybeStartRace = function() {
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
    player.setStartPosition(this.race[DATA_START_LOCATION]);
    player.setState(PlayerState.ACTIVE);
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

  this.messageBus.broadcast(payload);
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
    this.setState(GameState.RACE);
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


/** @private */
MapRacer.prototype.sendPlayerCount_ = function() {
  var payload = {
    type: MessageType.PLAYER_COUNT,
    count: Object.keys(this.players).length
  };

  this.messageBus.broadcast(payload);
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

  this.maybeStartRace();
};


/** @param {cast.receiver.CastMessageBus.Event} message The incoming message. */
MapRacer.prototype.onCastMessage = function(message) {
  console.dir(message);

  var data = message.data;
  switch (data[DATA_TYPE]) {
    case MessageType.REQUEST:
      this.onGameRequest(message.senderId, data);
      break;
    case MessageType.POSITION:
      if (message.senderId in this.players) {
        var location = new google.maps.LatLng(
            data.location.lat, data.location.lng);
        this.players[message.senderId].onPosition(location);
      }
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


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onConnect = function(client) {
  console.log('Client connected!');
  console.dir(client);

  this.players[client.data] = new Player(client.data, this);
  this.sendPlayerCount_();
  this.maybeStartRace();
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
  this.maybeFinishRace();
};
