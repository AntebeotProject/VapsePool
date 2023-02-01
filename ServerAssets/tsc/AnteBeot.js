"use strict";
exports.__esModule = true;
exports.AnteBeot = void 0;
var $ = require("jquery");
var pageLanguage_1 = require("./pageLanguage");
var cursor_1 = require("./cursor");
var captcha_1 = require("./captcha");
var ajax_1 = require("./ajax");
var AnteBeot = /** @class */ (function () {
    function AnteBeot(lang, cursorEnabled, captchaid) {
        if (lang === void 0) { lang = pageLanguage_1.defLanguage; }
        if (cursorEnabled === void 0) { cursorEnabled = false; }
        if (captchaid === void 0) { captchaid = null; }
        this.user = {
            isSigned: function (userSessionCookieName) {
                if (userSessionCookieName === void 0) { userSessionCookieName = "usession"; }
                return $.cookie(userSessionCookieName) != undefined;
            },
            unAuth: function (userSessionCookieName) {
                if (userSessionCookieName === void 0) { userSessionCookieName = "usession"; }
                $.removeCookie(userSessionCookieName);
            },
            doAuth: function (worknameField, workpassField, otpcodeField, ifsignedlocation) {
                if (worknameField === void 0) { worknameField = "#workname"; }
                if (workpassField === void 0) { workpassField = "#workpass"; }
                if (otpcodeField === void 0) { otpcodeField = "#otpcode"; }
                if (ifsignedlocation === void 0) { ifsignedlocation = "user.html"; }
                // $("input")[0]
                var name = $(worknameField).val();
                var pass = $(workpassField).val();
                var captcha = this.m_captcha.getValue();
                var ocode = $(otpcodeField).val();
                $.get("/signin", { 'workname': name, 'workpass': pass, 'captchaText': captcha, 'otpcode': ocode, 'lang': this.m_lang.lang() }).done(function (data) {
                    if (data.result === false) {
                        this.m_alertbox.showAlertBox(data.reason);
                    }
                    else
                        window.location.href = "user.html";
                    // showAlertBox("Succesfully. Reload the page", "Info", "snow")
                });
                this.m_captcha.clearValue();
                this.m_captcha.reloadCaptcha();
                if (true) {
                    window.location.href = ifsignedlocation;
                }
            }
        };
        this.m_lang = new pageLanguage_1.pageLanguage(lang);
        if (captchaid == null)
            this.m_captcha = new captcha_1.captcha();
        else
            this.m_captcha = new captcha_1.captcha(captchaid);
        if (cursorEnabled)
            this.m_cursor = new cursor_1.cursor();
        this.m_ajax = new ajax_1.ajax(this.m_lang);
    }
    AnteBeot.templates = /** @class */ (function () {
        function class_1() {
        }
        class_1.prototype.loadTemplate = function (name, setTitle, cID) {
            if (setTitle === void 0) { setTitle = name; }
            if (cID === void 0) { cID = $('#content'); }
            console.log("load templates/" + name + ".html");
            cID.empty();
            $(function () {
                cID.load("templates/" + name + ".html");
            });
            document.title = setTitle;
        };
        return class_1;
    }());
    return AnteBeot;
}());
exports.AnteBeot = AnteBeot;
