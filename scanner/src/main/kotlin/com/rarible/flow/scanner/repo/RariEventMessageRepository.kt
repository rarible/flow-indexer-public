package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.RariEventMessage
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

/**
 * Created by TimochkinEA at 04.06.2021
  *
 * Event messages about tracked events
 */
@Repository
interface RariEventMessageRepository : ReactiveMongoRepository<RariEventMessage, String> {

    @Query("{'event.timestamp': {\$gte: ?0}}")
    fun afterDate(afterDate: LocalDateTime): Flux<RariEventMessage>
}
