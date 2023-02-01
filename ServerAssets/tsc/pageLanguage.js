"use strict";
exports.__esModule = true;
exports.defLanguage = exports.pageLanguage = void 0;
var defLanguage = "ru_RU";
exports.defLanguage = defLanguage;
var pageLanguage = /** @class */ (function () {
    function pageLanguage(lang) {
        this.language = lang;
    }
    pageLanguage.prototype.change = function (nLang) {
        this.language = nLang;
    };
    pageLanguage.prototype.lang = function () {
        return this.language;
    };
    return pageLanguage;
}());
exports.pageLanguage = pageLanguage;
