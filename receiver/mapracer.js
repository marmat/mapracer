/**
 * MapRacer Receiver Code
 * @author kaktus621@gmail.com (Martin Matysiak)
 */


/** @const */
var NAMESPACE = 'urn:x-cast:de.martinmatysiak.mapracer';



/** @constructor */
MapRacer = function() {

  /** @type {Element} */
  this.mapEl = document.querySelector('#map');

  /** @type {Element} */
  this.timeEl = document.querySelector('#time');

  /** @type {Element} */
  this.targetEl = document.querySelector('#target');

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

  /** @type {google.maps.LatLng} */
  this.startLocation = {lat: 50.761596, lng: 6.137060};

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
};


/** @param {cast.receiver.CastMessageBus.Event} message The incoming message. */
MapRacer.prototype.onCastMessage = function(message) {
  console.log('Got a message!');
  console.dir(message);

  var payload = JSON.parse(message.data);
  if (payload.type == 'target') {
    this.targetEl.innerHTML = payload.title;
    this.map.setCenter(payload.location);
    new google.maps.Marker({
      map: this.map,
      position: payload.location,
      icon: this.targetIcon
    });
  } else if (payload.type == 'position') {
    var player = this.players[message.senderId];
    if (!!player) {
      // Note: the path wants actual g.m.LatLng objects, contrary to most
      // other methods...
      player.marker.setPosition(payload.location);
      player.path.getPath().push(
          new google.maps.LatLng(payload.location.lat, payload.location.lng));
    }
  }
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onConnect = function(client) {
  console.log('Client connected!');
  console.dir(client);

  var player = this.receiverManager.getSender(client.data);
  player.marker = new google.maps.Marker({
    map: this.map,
    position: this.startLocation,
    icon: this.playerIcon
  });

  player.path = new google.maps.Polyline({
    map: this.map,
    path: [this.startLocation],
    strokeColor: '#4390F7',
    strokeOpacity: 0.6,
    strokeWeight: 4
  });

  this.players[client.data] = player;
  console.dir(this.players);
};


/** @param {cast.receiver.CastReceiverManager.Event} client The client. */
MapRacer.prototype.onDisconnect = function(client) {
  console.log('Client disconnected!');
  console.dir(client);

  this.players[client.data].marker.setMap(null);
  this.players[client.data].path.setMap(null);
  delete this.players[client.data];
};
