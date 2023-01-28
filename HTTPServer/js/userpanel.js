function changeInputAddr(coin_name) 
{
	genNewAddress (coin_name)
	location.reload()
}
function session_refresh()
{
	let res = updateSession()
	showAlertBox(res.reason, "Info")
}
function changePass()
{
	let lpass = $("#last_pass").val() 
	let npass = $("#new_pass").val() 
	let res = changePassword(lpass, npass)
	showAlertBox(res.reason, "Info")
}
function outputMoney_()
{
let login = $("#outputLogin").val()
let pass =  $("#outputPassword").val()
let addr = $("#outputAddr").val()
let count_of_money = $("#outputCMoney").val()
let coin_name = $("#coins_for_output").val()
let captchaText = $("#captchaText").val()
let res = outputMoney(login, pass, addr, count_of_money, coin_name, captchaText)
showAlertBox(res.reason, "Info")
reloadCaptcha()
$("#captchaText").val("")
}
if (!userIsSigned())
{
	window.location.href = "/"
}
var userInfo = GetUserInfo()
$("p.Login").text("Login: "+userInfo.Login)
document.title = "Dashboard of " + userInfo.Login
for(name in userInfo.Balances) {
 let balance = userInfo.Balances[name]
 console.log(balance)
 let owner = balance["owner"] // self name
 let CoinName = balance["CoinName"]
 let inputAddress = balance["inputAddress"]
 let _balance = balance["balance"]
 //var isBlocked = balance["isBlocked"]
 $(".Balances").append("<li><p>" + CoinName + ": " + _balance + "<p>Input address: " + inputAddress + "</p>") 
}
var coins = getAllowCoins()
for (idx in coins) // forEach?
{
	let coin_name = coins[idx]
	let own_input_addr = getowninput(coin_name)
	$("#coins_for_output").append($('<option>', {
    		value: coin_name,
    		text: coin_name
	}));
	$(".coins").append("<li>" + coin_name +" </br><button onclick=\"changeInputAddr('"+coin_name+"')\">Change input. for now is " + own_input_addr + "</li>")
}