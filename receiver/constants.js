var NAMESPACE = 'urn:x-cast:de.martinmatysiak.mapracer';
var MIN_PLAYERS = 1;
var COUNTDOWN_DURATION = 5;
var SCORE_DURATION = 10;
var WIN_DISTANCE_THRESHOLD = 50; // in meters
var S_TO_MS = 1000;

var DATA_TYPE = 'type';
var DATA_ACTIVE = 'active';
var DATA_START_TIME = 'start_time';
var DATA_START_LOCATION = 'start_location';
var DATA_TARGET_LOCATION = 'target_location';
var DATA_TARGET_TITLE = 'target_title';

var MessageType = {
  REQUEST: 'request',
  START: 'start',
  POSITION: 'position',
  STOP: 'stop',
  PLAYER_COUNT: 'player_count',
  STATUS: 'status'
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
