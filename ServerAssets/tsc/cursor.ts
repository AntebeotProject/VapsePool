export class cursor {
	toDisableCurorElements = ["html", "input", "textarea"]
	disableDefaultCursor() {
		for (var t in this.toDisableCurorElements)
		{
			let element = this.toDisableCurorElements [t]
			$(element).css({cursor: "none"});

		}
	}
	enableDefaultCursor()
	{
		for (var t in this.toDisableCurorElements)
		{
			let element = this.toDisableCurorElements [t]
			$(element).css({cursor: "default"});

		}
	}
	loadingCursorEnable(cursorPath = "/CURSORS/Loading.gif") {
		// Have the cursor follow the mouse
		$(document).mousemove(function (e) {
				$(".pointer").css({ left: e.pageX, top: e.pageY });
		});
		if ($(".pointer").length == 0) $("html").append('<div class="pointer"><img src='+cursorPath+'></div>');
		else $(".pointer").css({display:"block"})
		this.disableDefaultCursor()
	}
	loadingCursorDisable()
	{
		$(".pointer").css({display:"none"})
		this.enableDefaultCursor()
	}
}