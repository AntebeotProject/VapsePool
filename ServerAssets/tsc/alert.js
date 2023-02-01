"use strict";
exports.__esModule = true;
exports.Alert = void 0;
var Alert = /** @class */ (function () {
    function Alert(alertTitleName, alertMsgName, alertBoxName) {
        if (alertTitleName === void 0) { alertTitleName = "#alertTitle"; }
        if (alertMsgName === void 0) { alertMsgName = "#alertMsg"; }
        if (alertBoxName === void 0) { alertBoxName = "#alertBox"; }
        this.alertBoxName = alertBoxName;
        this.alertMsgName = alertMsgName;
        this.alertTitleName = alertTitleName;
    }
    Alert.prototype.getAlertTitle = function () {
        return $(this.alertTitleName);
    };
    Alert.prototype.getAlertText = function () {
        return $(this.alertMsgName);
    };
    Alert.prototype.getAlertBox = function () {
        return $(this.alertBoxName);
    };
    Alert.prototype.showAlertBox = function (msg, title, color) {
        if (title === void 0) { title = "ERROR"; }
        if (color === void 0) { color = "#202020"; }
        var alertTitle = this.getAlertTitle();
        var alertText = this.getAlertText();
        var alertBox = this.getAlertBox();
        alertText.text(msg);
        alertTitle.text(title);
        console.log("show css for alert");
        alertBox.css({ 'background-color': color });
        alertBox.show();
    };
    Alert.prototype.hideAlertBox = function () {
        var alertTitle = this.getAlertTitle();
        var alertText = this.getAlertText();
        var alertBox = this.getAlertBox();
        alertText.text("");
        alertTitle.text("");
        alertBox.hide();
    };
    return Alert;
}());
exports.Alert = Alert;
