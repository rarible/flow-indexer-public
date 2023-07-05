package com.rarible.flow.scanner.listener

import com.rarible.blockchain.scanner.consumer.LogRecordMapper
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.flow.core.domain.BalanceLogRecordEvent
import com.rarible.flow.core.domain.GeneralFlowLogRecordEvent
import com.rarible.flow.scanner.model.LogRecordEventListeners

abstract class FlowLogListener<T>(
    val eventType: Class<T>,
    val eventMapper: LogRecordMapper<T>,
    val name: String,
    flowGroupId: String
) : LogRecordEventListener {

    override val groupId = flowGroupId

    override val id = LogRecordEventListeners.listenerId(name)
}

abstract class BalanceFlowLogListener(
    name: String,
    flowGroupId: String,
    environmentInfo: ApplicationEnvironmentInfo,
) : FlowLogListener<BalanceLogRecordEvent>(
    eventType = BalanceLogRecordEvent::class.java,
    eventMapper = BalanceLogRecordEvent.logRecordMapper(),
    name = name,
    flowGroupId = flowGroupId
)

abstract class GeneralFlowLogListener(
    name: String,
    flowGroupId: String,
    environmentInfo: ApplicationEnvironmentInfo,
) : FlowLogListener<GeneralFlowLogRecordEvent>(
    eventType = GeneralFlowLogRecordEvent::class.java,
    eventMapper = GeneralFlowLogRecordEvent.logRecordMapper(),
    name = name,
    flowGroupId = flowGroupId
)