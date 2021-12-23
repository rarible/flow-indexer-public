package com.rarible.flow.scanner.service

import com.mongodb.client.result.UpdateResult
import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.domain.Order
import com.rarible.flow.core.domain.OrderStatus
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.lte
import java.math.BigDecimal
import kotlin.reflect.KProperty

internal class BidServiceTest: FunSpec({

    val goodRepo = mockk<OrderRepository>("orderRepository") {
        coEvery {
            update(any(), any())
        } returns UpdateResult.acknowledged(10, 10, null)
    }

    test("should deactivate bids by balance") {
        BidService(goodRepo).deactivateBidsByBalance(
            Balance(FlowAddress("0x01"), "A.1234.FlowToken", 11.3.toBigDecimal())
        )

        coVerify {
            goodRepo.update(
                OrderFilter.OnlyBid *
                    OrderFilter.ByStatus(OrderStatus.ACTIVE) *
                    OrderFilter.ByMaker(FlowAddress("0x01")) *
                    OrderFilter.ByMakeValue(KProperty<BigDecimal>::gt, 11.3.toBigDecimal()),
                eq(
                    Update()
                        .set(Order::status.name, OrderStatus.INACTIVE)
                        .set(Order::makeStock.name, 11.3.toBigDecimal())
                )
            )
        }
    }

    test("should activate bids by balance") {
        BidService(goodRepo).activateBidsByBalance(
            Balance(FlowAddress("0x01"), "A.1234.FlowToken", 11.3.toBigDecimal())
        )

        coVerify {
            goodRepo.update(
                OrderFilter.OnlyBid *
                        OrderFilter.ByStatus(OrderStatus.INACTIVE) *
                        OrderFilter.ByMaker(FlowAddress("0x01")) *
                        OrderFilter.ByMakeValue(KProperty<BigDecimal>::lte, 11.3.toBigDecimal()),
                eq(
                    Update()
                        .set(Order::status.name, OrderStatus.ACTIVE)
                        .set(Order::makeStock.name, 11.3.toBigDecimal())
                )
            )
        }
    }

})