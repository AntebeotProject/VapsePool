export class captcha
{
	idOfCaptcha: string
	captchatextid: string
	constructor(idOfCaptcha: string = "catpcha", captchatextid: string = "captchaText")
	{
		this.idOfCaptcha = idOfCaptcha
	}
	reload()
	{
		    var captcha = document.getElementById(this.idOfCaptcha)
            captcha.setAttribute("src", "/captcha?w=get&" + new Date().getTime());
	}
	getValue() {
		var captcha = document.getElementById(this.captchatextid)
		return captcha.getAttribute("value")
	}
	clearValue()
	{
		var captcha = document.getElementById(this.captchatextid)
		return captcha.setAttribute("value", "")
	}
}