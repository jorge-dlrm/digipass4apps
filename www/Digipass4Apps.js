
var exec = require('cordova/exec');

var PLUGIN_NAME = 'Digipass4Apps';

function Digipass4Apps() { }


Digipass4Apps.prototype.deviceFingerprint = function (successCallback, errorCallback) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'deviceFingerprint');
}

Digipass4Apps.prototype.licenseActivation = function (successCallback, errorCallback, m2) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'licenseActivation', [{"m2": m2}]);
}

Digipass4Apps.prototype.getDeviceCode = function (successCallback, errorCallback, m1, staticVector) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'getDeviceCode', [{"m1": m1, "staticVector": staticVector}]);
}

Digipass4Apps.prototype.generateOTP = function (successCallback, errorCallback) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'generateOTP');
}

Digipass4Apps.prototype.saveInSecureStorage = function (successCallback, errorCallback, key, value) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'saveInSecureStorage', [{"key": key, "value": value}]);
}

Digipass4Apps.prototype.getFromSecureStorage = function (successCallback, errorCallback, key) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'getFromSecureStorage', [{"key": key}]);
}

Digipass4Apps.prototype.deleteSecureStorage = function (successCallback, errorCallback) {

      if (errorCallback == null) {
            errorCallback = function () {
            };
      }

      if (typeof errorCallback != "function") {
            console.log("El parametro de Error debe ser una función");
            return;
      }

      if (typeof successCallback != "function") {
            console.log("El parametro de Exito debe ser una función");
            return;
      }

      exec(successCallback, errorCallback, PLUGIN_NAME, 'deleteSecureStorage');
}


var Digipass4Apps = new Digipass4Apps();
module.exports = Digipass4Apps;
