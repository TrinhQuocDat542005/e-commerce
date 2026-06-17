package com.dat.ecommerce.notification_service.controller

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = ["*"])
class NotificationController {

    private val log = LoggerFactory.getLogger(NotificationController::class.java)

    // Thread-safe list to hold active SSE emitters
    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamNotifications(): SseEmitter {
        // Create emitter with no timeout (or 24h timeout to prevent early termination)
        val emitter = SseEmitter(24 * 60 * 60 * 1000L)
        emitters.add(emitter)

        log.info("🔌 New client subscribed to Notification SSE (Active: ${emitters.size})")

        emitter.onCompletion {
            emitters.remove(emitter)
            log.info("🔌 Client completed SSE stream (Active: ${emitters.size})")
        }

        emitter.onTimeout {
            emitters.remove(emitter)
            log.info("🔌 SSE stream timed out (Active: ${emitters.size})")
        }

        emitter.onError {
            emitters.remove(emitter)
            log.info("❌ SSE stream error occurred (Active: ${emitters.size})")
        }

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected successfully to Real-Time Notification Stream!"))
        } catch (e: Exception) {
            emitters.remove(emitter)
        }

        return emitter
    }

    fun dispatchNotification(message: String) {
        if (emitters.isEmpty()) return
        log.info("📢 Dispatching live notification to ${emitters.size} clients: $message")
        
        val deadEmitters = mutableListOf<SseEmitter>()
        for (emitter in emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message))
            } catch (e: Exception) {
                deadEmitters.add(emitter)
            }
        }
        
        if (deadEmitters.isNotEmpty()) {
            emitters.removeAll(deadEmitters)
            log.info("🧹 Swept away ${deadEmitters.size} dead emitters. Remaining: ${emitters.size}")
        }
    }
}
