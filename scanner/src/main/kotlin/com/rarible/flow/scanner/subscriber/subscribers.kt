package com.rarible.flow.scanner.subscriber

import com.rarible.blockchain.scanner.flow.model.FlowDescriptor


internal fun flowDescriptor(
    address: String,
    contract: String,
    events: Iterable<String>,
    startFrom: Long? = null,
    dbCollection: String,
) = FlowDescriptor(
    id = "${contract}Descriptor",
    events = events.map { "A.$address.$contract.$it" }.toSet(),
    collection = dbCollection,
    startFrom = startFrom
)