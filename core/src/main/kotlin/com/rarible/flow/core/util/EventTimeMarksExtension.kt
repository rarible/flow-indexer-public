package com.rarible.flow.core.util

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.dto.FlowEventTimeMarkDto
import com.rarible.protocol.dto.FlowEventTimeMarksDto
import java.time.Instant

private const val stage = "indexer"
private const val postfix = ""

fun EventTimeMarks.addIn(date: Instant? = null) = this.addIn(stage, postfix, date)
fun EventTimeMarks.addOut(date: Instant? = null) = this.addOut(stage, postfix, date)

fun EventTimeMarks.toDto(): FlowEventTimeMarksDto {
    return FlowEventTimeMarksDto(this.source, this.marks.map { FlowEventTimeMarkDto(it.name, it.date) })
}

fun offchainEventMarks() = EventTimeMarks("offchain").add("source").addIn()


