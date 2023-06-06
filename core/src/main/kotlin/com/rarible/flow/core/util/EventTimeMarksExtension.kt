package com.rarible.flow.core.util

import com.rarible.blockchain.scanner.framework.util.addIn
import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.FlowEventTimeMarkDto
import com.rarible.protocol.dto.FlowEventTimeMarksDto

fun EventTimeMarks.toDto(): FlowEventTimeMarksDto = FlowEventTimeMarksDto(
    source,
    marks.map { FlowEventTimeMarkDto(it.name, it.date) }
)

fun offchainEventMarks() = EventTimeMarks("offchain").add("source").addIn()