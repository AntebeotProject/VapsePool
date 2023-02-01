import * as $ from "jquery";
import {pageLanguage, defLanguage} from './pageLanguage';
import {Alert} from './alert';
import {cursor} from './cursor';
import {captcha} from './captcha';
import {ajax} from './ajax';

class AnteBeot {
	static templates = class {
		loadTemplate(name, setTitle = name, cID = $('#content'))
		{
			console.log("load templates/"+name+".html")
			cID.empty();
			$(function() {
					cID.load("templates/"+name+".html");
			});
			document.title = setTitle
		}
	}
	m_lang: pageLanguage
	m_captcha: captcha
	m_cursor: cursor
	m_alertbox: Alert
	m_ajax: ajax
	constructor(lang: string = defLanguage, cursorEnabled: boolean = false, captchaid: string = null) {
		this.m_lang = new pageLanguage(lang); 
		if (captchaid == null) this. m_captcha = new captcha(); else this. m_captcha = new captcha(captchaid);
		if (cursorEnabled) this.m_cursor = new cursor()
		this.m_ajax = new ajax(this.m_lang)
	}
	user = {
		isSigned(userSessionCookieName = "usession") 
		{
			return $.cookie(userSessionCookieName) != undefined 
		},
		unAuth(userSessionCookieName = "usession")
		{
			$.removeCookie(userSessionCookieName)
		},
		doAuth(worknameField: string = "#workname",
			workpassField: string = "#workpass",
			otpcodeField: string = "#otpcode", ifsignedlocation: string = "user.html") {
				// $("input")[0]
				var name = $(worknameField).val() 
				var pass = $(workpassField).val() 
				var captcha = this.m_captcha.getValue() 
				var ocode = $(otpcodeField).val() 
				$.get( "/signin", { 'workname': name, 'workpass': pass, 'captchaText': captcha, 'otpcode': ocode, 'lang': this.m_lang.lang() } ).done(function(data){
					if (data.result === false)
					{
						this.m_alertbox.showAlertBox(data.reason)	
					}else window.location.href = "user.html"
			// showAlertBox("Succesfully. Reload the page", "Info", "snow")
				});
				this.m_captcha.clearValue()
				this.m_captcha.reloadCaptcha()
				if (true)
				{
					window.location.href = ifsignedlocation
				}
		}
	}
	
}
export { AnteBeot };