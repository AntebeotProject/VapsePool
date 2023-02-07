var ulang = "ru_RU"
function changeULang(lang) {
	ulang = lang
}
function getAlertTitle() {
	return $("#alertTitle")
}
function getAlertText() {
	return $("#alertMsg")
}
function getAlertBox() {
	return $("#alertBox")
}
function expireCookie(name)
{
	var tmp = ""
	let cookies = document.cookie.split(";")
	for(idx in cookies) 
	{
		let cookie = cookies[idx]
		let pair = cookie.split("=")
		if (pair[0].trim() === name)
		{
			console.log("add expires")
			const d = new Date();
			d.setTime(d.getTime() + (1*24*60*60*1000));
			let expires = "expires="+ d.toUTCString();
			tmp = tmp + pair[0].trim()+"="+pair[1]+";"+ expires + ";path=/";
			continue
		}
		tmp = tmp + pair[0]+"="+pair[1]+";"
		// let idx = console.log(idx) 
	}	
}
function startTimerForSessionRefresh(seconds = 25)
{
	console.log("Session was updated")
	expireCookie("usession")
	// $.cookie("usession", val, { path: '/', expires: expDate })
	setTimeout(function() {updateSession(); startTimerForSessionRefresh(seconds);}, 1000 * seconds);
}
function reloadCaptcha() {
            var captcha = document.getElementById("catpcha")
            captcha.src="/captcha?w=get&" + new Date().getTime();
}
function showAlertBox(msg, title="ERROR", color = "#202020")
{
/*
	if ($("#alert").length == 0)
	{
		// NOT WORKS correctly
		$("html").append("<div id='alert'><p>ERROR!!!</p><b>"+msg+"</b><p><button onclick=\"hideAlertBox()\">OK</button></p></div>")
		// $("#alert")[0].innerHTML="<p>ERROR!!!</p><b>"+msg+"</b><p><button onclick=\"hideAlertBox()\">OK</button></p>"
	} 
*/
	var alertTitle = getAlertTitle()
	var alertText = getAlertText()
	var alertBox = getAlertBox()
	alertText.text(msg);
	alertTitle.text(title);
	console.log("show css for alert")
	alertBox.css( {'background-color': color} );
	alertBox.show()
	loadingCursorEnable()
}
function hideAlertBox()
{
	var alertTitle = getAlertTitle()
	var alertText = getAlertText()
	var alertBox = getAlertBox()

	alertText.text("")
	alertTitle.text("")

	alertBox .hide()
	loadingCursorDisable()
}
// https://www.w3schools.com/jquery/jquery_ajax_get_post.asp
function doAuth() 
{
	// $("input")[0]
	var name = $("#workname").val() 
	var pass = $("#workpass").val() 
	var captcha = $("#captchaText").val() 
	var ocode = $("#otpcode").val() 
	$.get( "/signin", { 'workname': name, 'workpass': pass, 'captchaText': captcha, 'otpcode': ocode, 'lang': ulang } ).done(function(data){
		if (data.result === false)
		{
			showAlertBox(data.reason)	
		}else window.location.href = "user.html"
// showAlertBox("Succesfully. Reload the page", "Info", "snow")
	});
	$("#captchaText").val("")
	reloadCaptcha()
	if (userIsSigned())
	{
		window.location.href = "user.html"
	}
}
function doRegistration()
{
	// $("input")[0]
	var name = $("#workname").val() 
	var pass = $("#workpass").val() 
	var pass2 = $("#workpass2").val() 
	var captcha = $("#captchaText").val() 
	$.get( "/registration", { workname: name, workpass: pass, workpass2: pass2, captchaText: captcha, 'lang': ulang  } ).done(function(data){
		if (data.result === false)
		{
			showAlertBox(data.reason)	
		}else window.location.href = "user.html"
	});
	$("#captchaText").val("")
	reloadCaptcha()
	if (userIsSigned())
	{
		window.location.href = "user.html"
	}
}
// not use const
var userSessionCookieName = "usession"
function unAuth() {
	$.removeCookie(userSessionCookieName )
}
function userIsSigned()
{
	return $.cookie("usession") != undefined 
}

// User Data
var userData = function (w) {
    var tmp = null;
    var reqdata = w
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
var genNewAddress = function (cryptocoin) { return userData ({'w':"genAddress", 'cryptocoin': cryptocoin}) };
var updateSession = function () { return userData ({'w':"updateSession"}) };
var changePassword = function (last_pass, new_pass) { return userData ({'w':"changePassword", 'last_pass':last_pass, 'new_pass':new_pass}) };
var getowninput  = function (cryptocoin) { return userData ({'w':"getowninput", 'cryptocoin': cryptocoin}) };
var userHave2FA = function() { return userData({"w":"checkOTP"}) };
var userHaveOTP = userHave2FA;
function GetUserInfo(w = {}) {
   return userData(w)
}
// api // TODO: var apiFunctional = {....}
var apiData = function (w) {
    var tmp = null;
    var reqdata = w
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
function getAllowCoins() {
 return apiData({'w':"getallowcoins"})
}
function outputMoney(login,password, output_address, count_of_money, coin_name, captchaData) {
 return apiData({'w':"output", 'acc': login, 'pass': password, 'oAdr': output_address, 'cMoney': count_of_money, 'coinname': coin_name, 'captchaText':captchaData, 'lang': ulang })
}

// exchange // TODO: var apiFunctional = {....}
var exchangeData = function (w) {
    var tmp = null;
    var reqdata = w
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
//function addOrderToSellCoin2Coin(toSellName, toSellPrice, toSellLimitMin, toSellLimitMax, toBuyName, toBuyPrice, toBuyLimitMin, toBuyLimitMax, msg = "", tIsBuyer = false)
function addOrderToSellCoin2Coin(SellNamecoin, BuyNamecoin, Price, VolumeStart, VolumeMax)
{
	console.log("Создаем ордер")
		console.log(VolumeMax) // gostcoin
	        console.log(VolumeStart) // 100
	        console.log(Price) // 100
	        console.log(SellNamecoin) // bitcoin
	        console.log(BuyNamecoin) // 0.000025
	return exchangeData ({'w': 'addOrderToSellCoin2Coin',
		'toGiveName': SellNamecoin,
		'toGetName': BuyNamecoin,
		'Price': Price,
		'VolumeStart': VolumeStart,
		'VolumeMax': VolumeMax,
	});
}
function getMyDoneTrade()
{
	return exchangeData ({'w': 'getMyDoneTrade'})
}
function getOwnOrders()
{
	return exchangeData ({'w': 'getOwnOrders'})
}
function getReviewsByAbout(who)
{
	return exchangeData ({'w': 'getReviewsByAbout', 'who': who})
}
function getReviewsByReviewer(who)
{
	return exchangeData ({'w': 'getReviewsByReviewer', 'who': who})
}
function getMyReviews()
{
	return exchangeData ({'w': 'getMyReviews'})
}
function addReview(id, text)
{
	return exchangeData ({'w': 'addReview', 'id': id, 'text': text})
}
//
function getOrders(active=true, offset = 0, lim = 25)
{
	return exchangeData ({'w': 'getOrders', 'a': active, 'offset': offset, 'lim': lim})
}
function getOrderByName(who, offset = 0, lim = 25)
{
	// offset, lim
	return exchangeData ({'w': 'getOrderByName', 'who': who, 'offset': offset, 'lim': lim})
}
function getTraderStats(who)
{
	return exchangeData ({'w': 'getTraderStats', 'who': who})
}
function remOrder(id)
{
 // 
  return exchangeData ({'w': 'removeMyOrderByID', 'id': id})
}
function changeActiveOrder(id, s=true)
{
	console.log(`changeActiveOrder ${id} to ${s}`)
	return exchangeData ({'w': 'changeActiveOrder', 'id': id, 's': s})
}
function doTrade(id, count = 1)
{
	return exchangeData ({'w': 'doTrade', 'count': count, 'id': id})
}