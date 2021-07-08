package com.rarible.flow.scanner

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Created by TimochkinEA at 07.07.2021
 */
@Profile("!no-kafka")
@RestController
@RequestMapping("resendEvents")
class ResendRariEventMessageController(
    private val service: ResendRariEventMessageService
) {

    @PostMapping
    fun startResendFromDate(@RequestBody date: LocalDateTime): Mono<Boolean> {
        service.resendToKafka(date)
        return Mono.just(true)
    }
}
