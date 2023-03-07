package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow

abstract class AbstractNftActivityTest  {
    protected val txManager = mockk<TxManager>()

    protected val logRepository = mockk<FlowLogRepository> {
        coEvery { findAfterEventIndex(any(), any(), any(), any()) } returns emptyFlow()
        coEvery { findBeforeEventIndex(any(), any(), any(), any()) } returns emptyFlow()
    }
    protected val properties = mockk<FlowListenerProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }
}