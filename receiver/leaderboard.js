


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
 * Adds a new player to the leaderboard.
 * @param {String} id The player's ID.
 * @param {String} displayName The name to show in the UI.
 * @param {number} sortValue The value after which items will be sorted.
 */
Leaderboard.prototype.add = function(id, displayName, sortValue) {
  if (id in this.entries) {
    return; // duplicate
  }

  var element = document.createElement('li');
  element.innerHTML = displayName;
  this.entries[id] = element;
  this.update(id, sortValue);
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
