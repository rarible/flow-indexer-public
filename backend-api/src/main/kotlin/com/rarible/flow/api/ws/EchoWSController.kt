package com.rarible.flow.api.ws

import com.rarible.flow.log.Log
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller

/**
 * Created by TimochkinEA at 08.07.2021
 */
@Controller
class EchoWSController {

    private val log by Log()

    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    fun echo(@Payload echo: String): String {
        log.info("Echo ws endpoint: $echo")
        return echo.uppercase()
    }

    @SubscribeMapping("/ping")
    fun ping(): String = "pong"
}
