
rootProject.name = "VAPSEPOOL"
include("untitled")
include("AsyncServer")
include("GOSTD")
include("GOSTD")
findProject(":GOSTD")?.name = "gostd"
include("TelegramBot")
