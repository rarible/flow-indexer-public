package com.rarible.flow.listener.handler.listeners

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.ItemHistoryRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.listener.service.ItemService
import com.rarible.flow.events.EventMessage
import com.rarible.flow.listener.handler.ProtocolEventPublisher
import com.rarible.flow.listener.service.OrderService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.util.*

@Component(WithdrawListener.ID)
class WithdrawListener(
    private val orderService: OrderService,
    private val protocolEventPublisher: ProtocolEventPublisher
) : SmartContractEventHandler {

    override suspend fun handle(
        eventMessage: EventMessage
    ): Unit = coroutineScope {
        val event = Withdraw(eventMessage.fields)

        orderService.cancelOrderByItemIdAndMaker(
            ItemId(eventMessage.eventId.collection(), event.id.toLong()),
            FlowAddress(event.from)
        )?.let {
            protocolEventPublisher.onUpdate(it)
        }
    }

    companion object {
        const val ID = "CommonNFT.Withdraw"

        class Withdraw(fields: Map<String, Any?>) {
            val id: String by fields
            val from: String by fields
        }
    }
}
