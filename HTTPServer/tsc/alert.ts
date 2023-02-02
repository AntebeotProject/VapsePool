export class Alert {
	alertTitleName: string
	alertMsgName: string
	alertBoxName: string
	constructor(alertTitleName: string = "#alertTitle",
		alertMsgName: string = "#alertMsg", alertBoxName: string = "#alertBox")
	{
		this.alertBoxName = alertBoxName
		this.alertMsgName = alertMsgName
		this.alertTitleName = alertTitleName
	}
	getAlertTitle() {
		return $(this.alertTitleName)
	}
	getAlertText() {
		return $(this.alertMsgName)
	}
	getAlertBox() {
		return $(this.alertBoxName)
	}
	showAlertBox(msg, title="ERROR", color = "#202020") {
		var alertTitle = this.getAlertTitle()
		var alertText = this.getAlertText()
		var alertBox = this.getAlertBox()
		alertText.text(msg);
		alertTitle.text(title);
		console.log("show css for alert")
		alertBox.css( {'background-color': color} );
		alertBox.show()
	}
	hideAlertBox() {
		var alertTitle = this.getAlertTitle()
		var alertText = this.getAlertText()
		var alertBox = this.getAlertBox()

		alertText.text("")
		alertTitle.text("")

		alertBox .hide()
	}
}