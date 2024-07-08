package com.pavedroad.templateservice.controllers

import com.pavedroad.templateservice.services.SSEService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class SseController(@Autowired val sseService: SSEService) {

    @CrossOrigin(origins = ["http://localhost:3000"])
    @GetMapping("/sse/subscribe")
    fun subscribe(): SseEmitter {
        return sseService.subscribe()
    }
}