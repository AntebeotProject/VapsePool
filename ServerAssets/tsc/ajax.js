"use strict";
exports.__esModule = true;
exports.ajax = void 0;
var ajax = /** @class */ (function () {
    function ajax(l) {
        this.m_lang = l;
    }
    ajax.userData = function (w) {
        var tmp = null;
        var reqdata = w;
        $.ajax({
            'async': false,
            'type': "GET",
            'global': false,
            'dataType': 'json',
            'url': "/user/",
            'data': reqdata,
            'success': function (data) {
                tmp = data;
            }
        });
        return tmp;
    };
    ajax.user = {
        genNewAddress: function (cryptocoin) {
            return this.userData({ 'w': "genAddress", 'cryptocoin': cryptocoin });
        },
        updateSession: function () {
            return this.userData({ 'w': "updateSession" });
        },
        changePassword: function (last_pass, new_pass) {
            return this.userData({ 'w': "changePassword", 'last_pass': last_pass, 'new_pass': new_pass });
        },
        getowninput: function (cryptocoin) {
            return this.userData({ 'w': "getowninput", 'cryptocoin': cryptocoin });
        }
    };
    // /api rest api
    ajax.apiData = function (w) {
        var tmp = null;
        var reqdata = w;
        $.ajax({
            'async': false,
            'type': "GET",
            'global': false,
            'dataType': 'json',
            'url': "/api/",
            'data': reqdata,
            'success': function (data) {
                tmp = data;
            }
        });
        return tmp;
    };
    ajax.api = {
        getAllowCoins: function () {
            return this.apiData({ 'w': "getallowcoins" });
        },
        outputMoney: function (login, password, output_address, count_of_money, coin_name, captchaData) {
            return this.apiData({ 'w': "output", 'acc': login, 'pass': password, 'oAdr': output_address, 'cMoney': count_of_money, 'coinname': coin_name, 'captchaText': captchaData, 'lang': this.m_lang.lang() });
        }
    };
    // exchange rest api
    ajax.exchangeData = function (w) {
        var tmp = null;
        var reqdata = w;
        $.ajax({
            'async': false,
            'type': "GET",
            'global': false,
            'dataType': 'json',
            'url': "/exchange/",
            'data': reqdata,
            'success': function (data) {
                tmp = data;
            }
        });
        return tmp;
    };
    // exchange rest api
    ajax.exchange = {
        addOrderToSellCoin2Coin: function (toSellName, toSellPrice, toSellLimitMin, toSellLimitMax, toBuyName, toBuyPrice, toBuyLimitMin, toBuyLimitMax, msg, tIsBuyer) {
            if (msg === void 0) { msg = ""; }
            if (tIsBuyer === void 0) { tIsBuyer = false; }
            console.log("Создаем ордер");
            console.log(toSellName);
            console.log(toSellPrice);
            console.log(toSellLimitMin);
            console.log(toSellLimitMax);
            console.log(toBuyName);
            console.log(toBuyPrice);
            console.log(toBuyLimitMin);
            console.log(msg);
            return this.exchangeData({ 'w': 'addOrderToSellCoin2Coin',
                'toSellName': toSellName,
                'toSellPrice': toSellPrice,
                'toSellLimitMin': toSellLimitMin,
                'toSellLimitMax': toSellLimitMax,
                'toBuyName': toBuyName,
                'toBuyPrice': toBuyPrice,
                'toBuyLimitMin': toBuyLimitMin,
                'toBuyLimitMax': toBuyLimitMax,
                'tIsBuyer': tIsBuyer,
                'msg': msg
            });
        },
        getMyDoneTrade: function () {
            return this.exchangeData({ 'w': 'getMyDoneTrade' });
        },
        getOwnOrders: function () {
            return this.exchangeData({ 'w': 'getOwnOrders' });
        },
        getReviewsByAbout: function (who) {
            return this.exchangeData({ 'w': 'getReviewsByAbout', 'who': who });
        },
        getReviewsByReviewer: function (who) {
            return this.exchangeData({ 'w': 'getReviewsByReviewer', 'who': who });
        },
        getMyReviews: function () {
            return this.exchangeData({ 'w': 'getMyReviews' });
        },
        addReview: function (id, text) {
            return this.exchangeData({ 'w': 'addReview', 'id': id, 'text': text });
        },
        getOrders: function (active, offset, lim) {
            if (active === void 0) { active = true; }
            if (offset === void 0) { offset = 0; }
            if (lim === void 0) { lim = 25; }
            return this.exchangeData({ 'w': 'getOrders', 'a': active, 'offset': offset, 'lim': lim });
        },
        getOrderByName: function (who, offset, lim) {
            if (offset === void 0) { offset = 0; }
            if (lim === void 0) { lim = 25; }
            // offset, lim
            return this.exchangeData({ 'w': 'getOrderByName', 'who': who, 'offset': offset, 'lim': lim });
        },
        getTraderStats: function (who) {
            return this.exchangeData({ 'w': 'getTraderStats', 'who': who });
        },
        remOrder: function (id) {
            // 
            return this.exchangeData({ 'w': 'removeMyOrderByID', 'id': id });
        },
        changeActiveOrder: function (id, s) {
            if (s === void 0) { s = true; }
            console.log("changeActiveOrder ".concat(id, " to ").concat(s));
            return this.exchangeData({ 'w': 'changeActiveOrder', 'id': id, 's': s });
        },
        doTrade: function (id, count) {
            if (count === void 0) { count = 1; }
            return this.exchangeData({ 'w': 'doTrade', 'count': count, 'id': id });
        }
    }; // end orders
    return ajax;
}()); // end of class
exports.ajax = ajax;
