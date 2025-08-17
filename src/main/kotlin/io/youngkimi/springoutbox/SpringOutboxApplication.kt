package io.youngkimi.springoutbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringOutboxApplication

fun main(args: Array<String>) {
    runApplication<SpringOutboxApplication>(*args)
}
