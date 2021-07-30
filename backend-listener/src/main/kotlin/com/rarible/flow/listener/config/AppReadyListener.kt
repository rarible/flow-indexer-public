package com.rarible.flow.listener.config

import com.rarible.flow.core.domain.ItemCollection
import com.rarible.flow.core.repository.ItemCollectionRepository
import org.onflow.sdk.FlowAddress
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class AppReadyListener(
    private val props: ListenerProperties,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val itemCollectionRepository: ItemCollectionRepository
): ApplicationListener<ApplicationReadyEvent> {

    /**
     * Save default item collection
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        itemCollectionRepository.save(ItemCollection(
            id = props.defaultItemCollection.id,
            owner = FlowAddress(props.defaultItemCollection.owner),
            name = props.defaultItemCollection.name,
            symbol = props.defaultItemCollection.symbol
        )).block()
    }
}
