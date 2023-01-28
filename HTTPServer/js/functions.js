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
function addOrderToSellCoin2Coin(toSellName, toSellPrice, toSellLimitMin, toSellLimitMax, toBuyName, toBuyPrice, toBuyLimitMin, toBuyLimitMax, msg = "", tIsBuyer = false)
{
	console.log("Создаем ордер")
	console.log(toSellName)
	console.log(toSellPrice)
	console.log(toSellLimitMin)
	console.log(toSellLimitMax)
	console.log(toBuyName)
	console.log(toBuyPrice)
	console.log(toBuyLimitMin)
	console.log(msg)
	return exchangeData ({'w': 'addOrderToSellCoin2Coin',
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
}
function getMyDoneTrade()
{
	return exchangeData ({'w': 'getMyDoneTrade'})
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