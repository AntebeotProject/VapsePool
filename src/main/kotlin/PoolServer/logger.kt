package org.antibiotic.pool.main.PoolServer

import java.io.File

object logger {
    private val log_file = File("logs.txt")
    fun add_log(s: String) {
        log_file.exists().let { if (!it) log_file.createNewFile() }
        log_file.appendText(s + "\r\n")
    }
}