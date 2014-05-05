


/**
 * @param {Element} container The element which will hold the leaderboard
 *   entries.
 * @constructor
 */
Leaderboard = function(container) {
  /** @private @type {Element} */
  this.container_ = container;

  /** @type {Object.<String, Element>} */
  this.entries = {};
};


/**
 * @param {boolean} fullscreen Whether to show the Leaderboard in a fullscreen
 *    mode.
 */
Leaderboard.prototype.setFullscreen = function(fullscreen) {
  this.container_.classList.toggle('fullscreen', fullscreen);
};


/**
 * Adds a new player to the leaderboard.
 * @param {String} id The player's ID.
 * @param {String=} opt_displayName The name to show in the UI. If not set, the
 *     ID will be used as displayName.
 * @param {number=} opt_sortValue The value after which items will be sorted.
 * @param {String=} opt_color Color to use for the player. Default: white.
 */
Leaderboard.prototype.add = function(id, opt_displayName, opt_sortValue,
    opt_color) {
  if (id in this.entries) {
    return; // duplicate
  }

  var element = document.createElement('li');
  element.innerHTML = opt_displayName || id;
  element.style.color = opt_color || 'white';

  this.entries[id] = element;
  this.update(id, opt_sortValue || Infinity);
};


/**
 * Updates a player's sortValue and re-sorts to maintain a correct ordering.
 * @param {String} id The affected player's ID.
 * @param {number} sortValue The player's new sortValue.
 */
Leaderboard.prototype.update = function(id, sortValue) {
  var element = this.entries[id];
  if (!element) {
    return; // does not exist
  }

  element.setAttribute('data-value', sortValue);

  // Find first entry that has a greater sortValue
  var ref = null;
  for (var i = 0; i < this.container_.children.length; i++) {
    var child = this.container_.children[i];
    if (sortValue < child.getAttribute('data-value')) {
      ref = child;
      break;
    }
  }

  if (!!ref) {
    this.container_.insertBefore(element, ref);
  } else {
    this.container_.appendChild(element);
  }
};


/**
 * Removes a player from the list.
 * @param {String} id The player's ID.
 */
Leaderboard.prototype.remove = function(id) {
  var element = this.entries[id];
  if (!element) {
    return; // does not exist
  }

  element.parentNode.removeChild(element);
  delete this.entries[id];
};
