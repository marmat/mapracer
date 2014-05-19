var NAMESPACE = 'urn:x-cast:de.martinmatysiak.mapracer';
var MIN_PLAYERS = 1;
var COUNTDOWN_DURATION = 5;
var SCORE_DURATION = 10;
var WIN_DISTANCE_THRESHOLD = 50; // in meters
var S_TO_MS = 1000;

var MessageType = {
  // incoming messages
  LOGIN: 'login',
  REQUEST: 'request',
  POSITION: 'position',
  // outgoing messages
  GAME_STATE: 'game_state',
  PLAYER_STATE: 'player_state'
};

var GameState = {
  INIT: 'init',
  LOAD: 'load',
  RACE: 'race',
  SCORES: 'scores'
};

var PlayerState = {
  ACTIVE: 'active',
  WAITING: 'waiting',
  FINISHED: 'finished'
};


function formatTime(timestamp) {
  var seconds = Math.floor((timestamp / 1000) % 60);
  var formatted = Math.floor(timestamp / 60000) + // minutes
      (seconds < 10 ? ':0' : ':') + seconds + // seconds
      '.' + timestamp % 1000; // milliseconds

  return formatted;
}
