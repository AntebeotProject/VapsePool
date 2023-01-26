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
            let captcha = document.getElementById("catpcha")
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
	let alertTitle = getAlertTitle()
	let alertText = getAlertText()
	let alertBox = getAlertBox()
	alertText.text(msg);
	alertTitle.text(title);
	console.log("show css for alert")
	alertBox.css( {'background-color': color} );
	alertBox.show()
	loadingCursorEnable()
}
function hideAlertBox()
{
	let alertTitle = getAlertTitle()
	let alertText = getAlertText()
	let alertBox = getAlertBox()

	alertText.text("")
	alertTitle.text("")

	alertBox .hide()
	loadingCursorDisable()
}
// https://www.w3schools.com/jquery/jquery_ajax_get_post.asp
function doAuth() 
{
	// $("input")[0]
	let name = $("#workname").val() 
	let pass = $("#workpass").val() 
	let captcha = $("#captchaText").val() 
	let ocode = $("#otpcode").val() 
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
	let name = $("#workname").val() 
	let pass = $("#workpass").val() 
	let pass2 = $("#workpass2").val() 
	let captcha = $("#captchaText").val() 
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
const userSessionCookieName = "usession"
function unAuth() {
	$.removeCookie(userSessionCookieName )
}
function userIsSigned()
{
	return $.cookie("usession") != undefined 
}

// User Data
let userData = function (w) {
    var tmp = null;
    let reqdata = w
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
let genNewAddress = function (cryptocoin) { return userData ({'w':"genAddress", 'cryptocoin': cryptocoin}) };
let updateSession = function () { return userData ({'w':"updateSession"}) };
let changePassword = function (last_pass, new_pass) { return userData ({'w':"changePassword", 'last_pass':last_pass, 'new_pass':new_pass}) };
let getowninput  = function (cryptocoin) { return userData ({'w':"getowninput", 'cryptocoin': cryptocoin}) };
function GetUserInfo(w = {}) {
   return userData(w)
}
// api // TODO: var apiFunctional = {....}
let apiData = function (w) {
    var tmp = null;
    let reqdata = w
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