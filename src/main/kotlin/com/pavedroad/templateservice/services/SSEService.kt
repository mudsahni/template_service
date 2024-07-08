package com.pavedroad.templateservice.services

import com.pavedroad.templateservice.logging.LogEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SSEService {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    fun subscribe(): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)
        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }

        return emitter
    }

    fun sendEvent(eventName: String, data: LogEvent) {
        val deadEmitters = mutableListOf<SseEmitter>()
        log.info("Attempting to send event: $eventName with data: $data")
        log.info("Total active emitters: ${emitters.size}")

        emitters.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data))
            } catch (e: Exception) {
                deadEmitters.add(emitter)
            }
        }
        emitters.removeAll(deadEmitters.toSet())
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    fun sendKeepAlive() {
        sendEvent("keepalive", LogEvent(System.currentTimeMillis(), "", "", "keep alive"))
    }


}