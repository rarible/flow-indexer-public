package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowTransaction
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 25.05.2021
 */
//@Component
class BlockInsertListener(private val messageTemplate: SimpMessagingTemplate) :
    AbstractMongoEventListener<FlowBlock>() {

    override fun onAfterSave(event: AfterSaveEvent<FlowBlock>) {
        messageTemplate.convertAndSend("/topic/block", event.source)
    }
}

//@Component
class TransactionListener(private val messageTemplate: SimpMessagingTemplate) :
    AbstractMongoEventListener<FlowTransaction>() {

    override fun onAfterSave(event: AfterSaveEvent<FlowTransaction>) {
        super.onAfterSave(event)
        messageTemplate.convertAndSend("/topic/transaction", event.source)
    }
}
