package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.RariEventMessageCaught
import com.rarible.flow.scanner.repo.RariEventMessageRepository
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 07.07.2021
 */
@Component
class SaveRariEventMessageAfterCaught(
    private val rariEventMessageRepository: RariEventMessageRepository
): ApplicationListener<RariEventMessageCaught> {

    override fun onApplicationEvent(event: RariEventMessageCaught) {
        rariEventMessageRepository.save(
            event.message
        ).subscribe()
    }
}
