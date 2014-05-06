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

  /** @type {Race} */
  this.race = new Race();

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


/** @private */
MapRacer.prototype.broadcastState_ = function() {
  this.messageBus.broadcast({
    type: MessageType.GAME_STATE,
    players: Object.keys(this.players).length,
    race: this.race.serialize(),
    state: this.state
  });
};


/** @param {GameState} state The new state to show. */
MapRacer.prototype.setState = function(state) {
  this.state = state;
  switch (state) {
    case GameState.INIT:
      this.race = new Race();
      this.splashEl.style.opacity = 1;
      this.titleEl.style.display = 'inline';
      this.titleEl.innerHTML = 'MapRacer';
      this.leaderboard.setFullscreen(false);
      // Reset all connected clients
      for (var id in this.players) {
        this.players[id].setState(PlayerState.WAITING);
      }
      break;
    case GameState.LOAD:
      this.countdownInterval_ = setInterval(this.countdown_.bind(this), 1000);
      this.titleEl.innerHTML = 'Get Ready!';
      break;
    case GameState.RACE:
      clearInterval(this.countdownInterval_);
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
      Object.keys(this.players).length < MIN_PLAYERS) {
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
  var difference = Date.now() - this.race.startTime;
  this.timeEl.innerHTML = formatTime(difference);
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
  switch (data.type) {
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
  // TODO: ignore request if race is already in progress
  this.race = new Race(payload);

  // Check back with StreetView if the provided locations are valid
  var targetLocation = this.race.targetLocation || this.pickTarget();
  var startLocation = this.race.startLocation || this.pickStart(targetLocation);

  // Invalidate race locations for now
  this.race.targetLocation = null;
  this.race.startLocation = null;

  this.streetViewService.getPanoramaByLocation(targetLocation, 50,
      this.onStreetViewLocation.bind(this, 'targetLocation'));

  this.streetViewService.getPanoramaByLocation(startLocation, 50,
      this.onStreetViewLocation.bind(this, 'startLocation'));
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onConnect = function(client) {
  console.log('Client connected!');
  console.dir(client);

  this.players[client.data] = new Player(client.data, this);
  this.broadcastState_();
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

  this.broadcastState_();
  this.maybeFinishRace();
};
