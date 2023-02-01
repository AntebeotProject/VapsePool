"use strict";
exports.__esModule = true;
exports.captcha = void 0;
var captcha = /** @class */ (function () {
    function captcha(idOfCaptcha, captchatextid) {
        if (idOfCaptcha === void 0) { idOfCaptcha = "catpcha"; }
        if (captchatextid === void 0) { captchatextid = "captchaText"; }
        this.idOfCaptcha = idOfCaptcha;
    }
    captcha.prototype.reload = function () {
        var captcha = document.getElementById(this.idOfCaptcha);
        captcha.setAttribute("src", "/captcha?w=get&" + new Date().getTime());
    };
    captcha.prototype.getValue = function () {
        var captcha = document.getElementById(this.captchatextid);
        return captcha.getAttribute("value");
    };
    captcha.prototype.clearValue = function () {
        var captcha = document.getElementById(this.captchatextid);
        return captcha.setAttribute("value", "");
    };
    return captcha;
}());
exports.captcha = captcha;
