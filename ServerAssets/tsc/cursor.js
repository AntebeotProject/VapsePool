"use strict";
exports.__esModule = true;
exports.cursor = void 0;
var cursor = /** @class */ (function () {
    function cursor() {
        this.toDisableCurorElements = ["html", "input", "textarea"];
    }
    cursor.prototype.disableDefaultCursor = function () {
        for (var t in this.toDisableCurorElements) {
            var element = this.toDisableCurorElements[t];
            $(element).css({ cursor: "none" });
        }
    };
    cursor.prototype.enableDefaultCursor = function () {
        for (var t in this.toDisableCurorElements) {
            var element = this.toDisableCurorElements[t];
            $(element).css({ cursor: "default" });
        }
    };
    cursor.prototype.loadingCursorEnable = function (cursorPath) {
        if (cursorPath === void 0) { cursorPath = "/CURSORS/Loading.gif"; }
        // Have the cursor follow the mouse
        $(document).mousemove(function (e) {
            $(".pointer").css({ left: e.pageX, top: e.pageY });
        });
        if ($(".pointer").length == 0)
            $("html").append('<div class="pointer"><img src=' + cursorPath + '></div>');
        else
            $(".pointer").css({ display: "block" });
        this.disableDefaultCursor();
    };
    cursor.prototype.loadingCursorDisable = function () {
        $(".pointer").css({ display: "none" });
        this.enableDefaultCursor();
    };
    return cursor;
}());
exports.cursor = cursor;
