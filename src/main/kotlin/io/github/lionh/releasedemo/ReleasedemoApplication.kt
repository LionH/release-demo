package io.github.lionh.releasedemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReleasedemoApplication

fun main(args: Array<String>) {
	runApplication<ReleasedemoApplication>(*args)
}
