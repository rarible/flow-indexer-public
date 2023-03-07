package com.rarible.flow.scanner.activity.nft

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.repository.FlowLogRepository
import com.rarible.flow.scanner.TxManager
import com.rarible.flow.scanner.config.FlowListenerProperties
import io.mockk.every
import io.mockk.mockk

abstract class AbstractNftActivityTest  {

    protected val logRepository = mockk<FlowLogRepository>()
    protected val txManager = mockk<TxManager>()
    protected val properties = mockk<FlowListenerProperties> {
        every { chainId } returns FlowChainId.MAINNET
    }
}