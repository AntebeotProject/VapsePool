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
function showAlertBox(msg, title="ERROR", color = "grey")
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

	$.get( "/signin", { workname: name, workpass: pass, captchaText: captcha } ).done(function(data){
		if (data.result === false)
		{
			showAlertBox(data.reason)	
		}else showAlertBox("You are succesfully signin. go to main page now", "Info", "snow")
	});
	$("#captchaText").val("")
	reloadCaptcha()
	if (userIsSigned())
	{
		window.location.href = "/user.html"
	}
}
function doRegistration()
{
	// $("input")[0]
	let name = $("#workname").val() 
	let pass = $("#workpass").val() 
	let pass2 = $("#workpass2").val() 
	let captcha = $("#captchaText").val() 
	$.get( "/registration", { workname: name, workpass: pass, workpass2: pass2, captchaText: captcha } ).done(function(data){
		if (data.result === false)
		{
			showAlertBox(data.reason)	
		}else showAlertBox("You are succesfully signin. go to main page now", "Info", "snow")
	});
	$("#captchaText").val("")
	reloadCaptcha()
	if (userIsSigned())
	{
		window.location.href = "/user.html"
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