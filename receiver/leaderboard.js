


/**
 * @param {Element} container The element which will hold the leaderboard
 *   entries.
 * @constructor
 */
Leaderboard = function(container) {
  /** @private @type {Element} */
  this.container_ = container;

  /** @type {Object.<string, PlayerInfo>} */
  this.players = {};

  /** @type {Array.<PlayerInfo>} Players sorted by score. */
  this.scores = [];

  /**
   * Placeholder for a change callback. Might be overridden by external classes.
   * Only one client can listen to events at a time. Sufficient for now. Should
   * be changed if we want more in the future.
   * @type {function()}
   */
  this.onLeaderboardChanged = function() {};
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
 * @param {string} id The player's ID.
 * @param {string=} opt_displayName The name to show in the UI. If not set, the
 *     ID will be used as displayName.
 * @param {number=} opt_sortValue The value after which items will be sorted.
 * @param {string=} opt_color Color to use for the player. Default: white.
 */
Leaderboard.prototype.add = function(id, opt_displayName, opt_sortValue,
    opt_color) {

  // only create an element if it's truly new
  if (!(id in this.players)) {
    var element = document.createElement('li');
    element.innerHTML = opt_displayName || id;
    element.style.color = opt_color || 'white';
    this.players[id] = {
      id: id,
      name: element.innerHTML,
      score: opt_sortValue || Infinity,
      element: element
    };
    console.dir(this.players[id]);
    this.scores.push(this.players[id]);
  }

  this.update(id, opt_sortValue || Infinity, opt_displayName);
  this.renderList_();
};


/**
 * Updates a player's sortValue and re-sorts to maintain a correct ordering.
 * @param {string} id The affected player's ID.
 * @param {number} sortValue The player's new sortValue.
 * @param {string=} opt_displayName If set, the player's name will be updated
 *    to the given value.
 */
Leaderboard.prototype.update = function(id, sortValue, opt_displayName) {
  var player = this.players[id];
  if (!player) {
    return; // does not exist
  }

  var currentIndex = this.scores.indexOf(player);
  this.scores.splice(currentIndex, 1);
  player.score = sortValue;

  if (!!opt_displayName) {
    player.element.innerHTML = opt_displayName;
  }

  // Find the first player with a greater score
  for (var target = 0;
      target < this.scores.length && this.scores[target].score <= sortValue;
      target++);

  // And put the current player into the right place
  this.scores.splice(target, 0, player);

  // Rerender if necessary
  if (target != currentIndex) {
    this.renderList_();
  }
};


/**
 * Removes a player from the list.
 * @param {string} id The player's ID.
 */
Leaderboard.prototype.remove = function(id) {
  var player = this.players[id];
  if (!player) {
    return; // does not exist
  }

  delete this.players[id];

  player.element.parentNode.removeChild(player.element);
  this.scores.splice(this.scores.indexOf(player), 1);
  this.renderList_();
};


/** @private */
Leaderboard.prototype.renderList_ = function() {
  this.container_.innerHTML = '';
  for (var i = 0; i < this.scores.length; i++) {
    this.container_.appendChild(this.scores[i].element);
  }

  this.onLeaderboardChanged();
};


/** @return {List.<Object>} The ordered list of players in the leaderboard. */
Leaderboard.prototype.getOrderedList = function() {
  // Make sure to remove the reference to the HTML element
  return this.scores.map(function(player) {
    return {
      id: player.id,
      name: player.name,
      score: player.score
    };
  });
};
