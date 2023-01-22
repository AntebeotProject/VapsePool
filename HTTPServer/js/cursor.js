var toDisableCurorElements = ["html", "input", "textarea"]
function disableDefaultCursor() {
	for (t in toDisableCurorElements)
	{
		let element = toDisableCurorElements [t]
		$(element).css({cursor: "none"});

	}
}
function enableDefaultCursor()
{
	for (t in toDisableCurorElements)
	{
		let element = toDisableCurorElements [t]
		$(element).css({cursor: "default"});

	}
}
function loadingCursorEnable(cursorPath = "/CURSORS/Loading.gif") {
	// Have the cursor follow the mouse
	$(document).mousemove(function (e) {
    		$(".pointer").css({ left: e.pageX, top: e.pageY });
	});
	if ($(".pointer").length == 0) $("html").append('<div class="pointer"><img src='+cursorPath+'></div>');
	else $(".pointer").css({display:"block"})
	disableDefaultCursor()
}
function loadingCursorDisable()
{
	$(".pointer").css({display:"none"})
	enableDefaultCursor()
}