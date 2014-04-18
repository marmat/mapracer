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
  this.castReceiverManager = null;

  /** @type {cast.receiver.CastMessageBus} */
  this.castMessageBus = null;

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
  this.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();
  this.castMessageBus = this.castReceiverManager.getCastMessageBus(NAMESPACE);
  this.castMessageBus.onMessage = this.onCastMessage.bind(this);
  this.castReceiverManager.start();
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
    new google.maps.Marker({
      map: this.map,
      position: payload.location,
      icon: this.playerIcon
    });
  }
};
