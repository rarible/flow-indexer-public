package com.rarible.flow.scanner

import com.rarible.flow.scanner.model.FlowTransaction

/**
 * Created by TimochkinEA at 01.07.2021
 */
interface IFlowEventAnalyzer {
    /**
     * Analysis of the transaction for events of interest
     */
    fun analyze(tx: FlowTransaction)
}
