package com.rarible.flow.scanner.subscriber

import com.rarible.flow.core.repository.BalanceRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk

internal class FungibleTokenSubscriberTest: FunSpec({

    val balanceRepo = mockk<BalanceRepository> {
        every {  }
    }

})