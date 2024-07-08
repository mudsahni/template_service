package com.pavedroad.templateservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class TemplateserviceApplication

fun main(args: Array<String>) {
	runApplication<TemplateserviceApplication>(*args)
}
