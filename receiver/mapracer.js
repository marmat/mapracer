/**
 * MapRacer Receiver Code
 * @author kaktus621@gmail.com (Martin Matysiak)
 */


/** @const */
var NAMESPACE = 'urn:x-cast:de.martinmatysiak.mapracer';
var MIN_PLAYERS = 1;
var COUNTDOWN_DURATION = 5;

var DATA_TYPE = 'type';
var DATA_ACTIVE = 'active';
var DATA_START_TIME = 'start_time';
var DATA_START_LOCATION = 'start_location';
var DATA_TARGET_LOCATION = 'target_location';
var DATA_TARGET_TITLE = 'target_title';



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

  /** @type {cast.receiver.CastReceiverManager} */
  this.receiverManager = null;

  /** @type {cast.receiver.CastMessageBus} */
  this.messageBus = null;

  /** @type {google.maps.Map} */
  this.map = null;

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

  /** @type {Object.<string, *>} */
  this.race = null;

  /**
   * Map from Sender ID to the Sender object containing all connected devices.
   * @type {Object.<string, cast.receiver.system.Sender>}
   */
  this.players = {};

  this.initializeCast_();
  this.initializeMap_();
  this.maybeHideSplashScreen_();
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
};


/** @private */
MapRacer.prototype.maybeHideSplashScreen_ = function() {
  if (!!this.race && Object.keys(this.players).length >= MIN_PLAYERS) {
    this.titleEl.innerHTML = 'Get Ready!';
    this.countdownInterval_ = setInterval(this.countdown_.bind(this), 1000);
  }
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
    this.startRace_();
    clearInterval(this.countdownInterval_);
  }
};


/** @private */
MapRacer.prototype.startRace_ = function() {
  this.splashEl.style.opacity = '0';
  this.race[DATA_START_TIME] = Date.now();
  this.race[DATA_ACTIVE] = true;

  setInterval(this.updateTimer.bind(this), 10);
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


/** @param {cast.receiver.CastMessageBus.Event} message The incoming message. */
MapRacer.prototype.onCastMessage = function(message) {
  console.dir(message);

  var payload = JSON.parse(message.data);
  switch (payload[DATA_TYPE]) {
    case 'request':
      this.onGameRequest(message.senderId, payload);
      break;
    case 'position':
      this.onPosition(message.senderId, payload);
      break;
    case 'start':
      // TODO
      break;
    case 'stop':
      // TODO
      break;
  }
};


/**
 * @param {String} senderId The player's ID.
 * @param {Object} payload The message payload.
 */
MapRacer.prototype.onGameRequest = function(senderId, payload) {

  var race = {};
  race[DATA_TARGET_TITLE] = payload[DATA_TARGET_TITLE] || 'the Finish';

  if (!!payload[DATA_TARGET_LOCATION]) {
    race[DATA_TARGET_LOCATION] = new google.maps.LatLng(
        payload[DATA_TARGET_LOCATION].lat,
        payload[DATA_TARGET_LOCATION].lng);
  } else {
    // Pick an interesting location at random
    // TODO
  }

  if (!!payload[DATA_START_LOCATION]) {
    race[DATA_START_LOCATION] = new google.maps.LatLng(
        payload[DATA_START_LOCATION].lat,
        payload[DATA_START_LOCATION].lng);
  } else {
    // Pick a location somewhat close to the target
    // TODO
  }

  this.race = race;
  this.targetEl.innerHTML = race[DATA_TARGET_TITLE];
  this.map.setCenter(race[DATA_START_LOCATION]);

  var raceBounds = new google.maps.LatLngBounds();
  raceBounds.extend(race[DATA_START_LOCATION]);
  raceBounds.extend(race[DATA_TARGET_LOCATION]);
  this.map.fitBounds(raceBounds);

  new google.maps.Marker({
    map: this.map,
    position: race[DATA_TARGET_LOCATION],
    icon: this.targetIcon
  });

  // Reposition all players
  for (var playerId in this.players) {
    this.players[playerId].marker.setPosition(race[DATA_START_LOCATION]);
    this.players[playerId].path.setPath([race[DATA_START_LOCATION]]);
  }

  // Broadcast the game start event (TODO move somewhere else)
  payload.type = 'start';
  this.messageBus.broadcast(JSON.stringify(payload));

  this.maybeHideSplashScreen_();
};


/**
 * @param {String} senderId The player's ID.
 * @param {Object} payload The message payload.
 */
MapRacer.prototype.onPosition = function(senderId, payload) {
  var player = this.players[senderId];
  if (!!player) {
    // Note: the path wants actual g.m.LatLng objects, contrary to most
    // other methods...
    player.marker.setPosition(payload.location);
    player.path.getPath().push(
        new google.maps.LatLng(payload.location.lat, payload.location.lng));
  }
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

  this.players[client.data] = player;
  this.maybeHideSplashScreen_();
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onDisconnect = function(client) {
  console.log('Client disconnected!');
  console.dir(client);

  this.players[client.data].marker.setMap(null);
  this.players[client.data].path.setMap(null);
  delete this.players[client.data];
};
