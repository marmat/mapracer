


/**
 * @param {Element} container The container in which to render the countdown.
 *     Should not contain any other nodes.
 * @constructor
 */
Countdown = function(container) {
  /** @type {Element} */
  this.container = container;

  /** @type {number} */
  this.currentCount = null;

  /** @type {Array.<function()>} */
  this.finishCallbacks = [];

  /** @private @type {number?} */
  this.timer_ = null;
};


/** @param {number} duration The duration in seconds that shall be counted. */
Countdown.prototype.start = function(duration) {
  if (!!this.timer_) {
    return; // already running
  }

  this.currentCount = duration;
  this.timer_ = setInterval(this.count_.bind(this), 1000);
};


/** Aborts the counting down */
Countdown.prototype.abort = function() {
  if (!this.timer_) {
    return; // countdown not running
  }

  clearInterval(this.timer_);
  this.timer_ = null;
  this.container.innerHTML = '';
};


/** @param {function()} callback Will be called when the countdown finishes. */
Countdown.prototype.addFinishCallback = function(callback) {
  this.finishCallbacks.push(callback);
};


/** @private */
Countdown.prototype.count_ = function() {
  if (this.currentCount == 0) {
    this.finishCallbacks.forEach(function(callback) { callback(); });
    this.abort();
    return;
  }

  this.container.innerHTML = '';
  var counter = document.createElement('span');
  counter.innerHTML = this.currentCount;
  counter.className = 'counter';

  this.container.appendChild(counter);
  this.currentCount--;
};
