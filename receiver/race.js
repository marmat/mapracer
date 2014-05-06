


/**
 * @param {Object=} opt_serializedInstance If given, we will populate our data
 *    fields from the given object.
 * @constructor
 */
Race = function(opt_serializedInstance) {
  // In order to avoid errors in the following, we replace an undefined optional
  // parameter by an empty object.
  opt_serializedInstance = opt_serializedInstance || {};

  /** @type {string} */
  this.title = opt_serializedInstance.target_title || '';

  /** @type {google.maps.LatLng} */
  this.targetLocation = null;
  if (!!opt_serializedInstance.target_location) {
    this.targetLocation = new google.maps.LatLng(
        opt_serializedInstance.target_location.lat,
        opt_serializedInstance.target_location.lng);
  }

  /** @type {google.maps.LatLng} */
  this.startLocation = null;
  if (!!opt_serializedInstance.start_location) {
    this.startLocation = new google.maps.LatLng(
        opt_serializedInstance.start_location.lat,
        opt_serializedInstance.start_location.lng);
  }

  /** @type {number} */
  this.startTime = 0;
};


/** @return {Object} A simple JS object that can be shipped through the web. */
Race.prototype.serialize = function() {
  return {
    start_location: !this.startLocation ? null : {
      lat: this.startLocation.lat(),
      lng: this.startLocation.lng()
    },
    start_time: this.startTime,
    target_location: !this.targetLocation ? null : {
      lat: this.targetLocation.lat(),
      lng: this.targetLocation.lng()
    },
    target_title: this.title
  };
};
