package com.rarible.flow.api.service

import com.nftco.flow.sdk.FlowAddress
import com.rarible.flow.api.BaseIntegrationTest
import com.rarible.flow.api.IntegrationTest
import com.rarible.flow.core.domain.*
import com.rarible.flow.core.repository.OrderFilter
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.randomLong
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

@IntegrationTest
internal class OrderServiceTest : BaseIntegrationTest() {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderService: OrderService

    @BeforeEach
    fun beforeEach() {
        orderRepository.deleteAll().block()
    }

    @Test
    fun `should return by status with correct sort`() = runBlocking<Unit> {
        (1..10).map {
            orderRepository.coSave(
                createOrder().copy(status = if (it % 3 != 0) OrderStatus.ACTIVE else OrderStatus.INACTIVE)
            )
        }

        val list = orderService.findAllByStatus(
            listOf(OrderStatus.ACTIVE), null, 3, OrderFilter.Sort.EARLIEST_FIRST
        ).toList()
        list should {
            it shouldBeSortedWith Comparator.comparing(Order::createdAt)
            it shouldHaveSize 3
        }

        shouldReadAllByOne(
            { cont -> orderService.findAllByStatus(
                listOf(OrderStatus.ACTIVE), cont, 1, OrderFilter.Sort.EARLIEST_FIRST
            ).toList() },
            7, 0, cmp = Comparator.comparing(Order::createdAt), sort = OrderFilter.Sort.EARLIEST_FIRST
        )

        shouldReadAllByOne(
            { cont -> orderService.findAllByStatus(
                listOf(OrderStatus.INACTIVE), cont, 1, OrderFilter.Sort.LATEST_FIRST
            ).toList() },
            3, 0, cmp = Comparator.comparing(Order::createdAt).reversed(), sort = OrderFilter.Sort.LATEST_FIRST
        )

        shouldReadAllByOne(
            { cont -> orderService.findAllByStatus(
                listOf(OrderStatus.INACTIVE, OrderStatus.ACTIVE), cont, 10, OrderFilter.Sort.LATEST_FIRST
            ).toList() },
            10, 0, cmp = Comparator.comparing(Order::createdAt).reversed(), sort = OrderFilter.Sort.LATEST_FIRST
        )
    }

    @Test
    fun `should return all with correct sort`() = runBlocking<Unit> {
        (1..10).map {
            orderRepository.coSave(
                createOrder().copy(status = if (it % 3 != 0) OrderStatus.ACTIVE else OrderStatus.INACTIVE)
            )
        }

        shouldReadAllByOne(
            { cont -> orderService.findAll(
                cont, 1, OrderFilter.Sort.EARLIEST_FIRST
            ).toList() },
            10, 0, cmp = Comparator.comparing(Order::createdAt), sort = OrderFilter.Sort.EARLIEST_FIRST
        )

        shouldReadAllByOne(
            { cont -> orderService.findAll(
                cont, 1, OrderFilter.Sort.LATEST_FIRST
            ).toList() },
            10, 0, cmp = Comparator.comparing(Order::createdAt).reversed(), sort = OrderFilter.Sort.LATEST_FIRST
        )
    }

    @Test
    fun `should getSellOrdersByItemAndStatus with correct sort`() = runBlocking<Unit> {
        (1..10).map {
            val mod = (it % 5 + 1).toLong()
            // tokeIds: 1 2 3 4 5 1 2 3 4 5
            // maker:   1 2 3 4 1 2 3 4 1 2
            // status:  A A I A A I A A I A
            orderRepository.coSave(
                createOrder().copy(
                    status = if (it % 3 != 0) OrderStatus.ACTIVE else OrderStatus.INACTIVE,
                    itemId = ItemId("A", mod),
                    maker = FlowAddress("0x0${it % 4 + 1}"),
                    amount = mod.toBigDecimal(),
                    take = FlowAssetFungible("A.$mod.Flow", mod.toBigDecimal())
                )
            )
        }

        //Add one bid
        orderRepository.coSave(
            createOrder().copy(
                itemId = ItemId("A", 1),
                make = FlowAssetFungible("FLOW", 100.toBigDecimal()),
                take = FlowAssetNFT("A", BigDecimal.ONE, 1)
            )
        )

        // maker is null, currency is null
        shouldReadAllByOne(
            { cont -> orderService.getSellOrdersByItemAndStatus(
                ItemId("A", 1), null, null, emptyList(), cont, 1, OrderFilter.Sort.MAKE_PRICE_ASC
            ).toList() },
            2, 0, cmp = Comparator.comparing { o -> o.make.value }, sort = OrderFilter.Sort.MAKE_PRICE_ASC
        )

        shouldReadAllByOne(
            { cont -> orderService.getSellOrdersByItemAndStatus(
                ItemId("A", 5), FlowAddress("0x01"), null, emptyList(), cont, 1, OrderFilter.Sort.EARLIEST_FIRST
            ).toList() },
            1, 0, cmp = Comparator.comparing(Order::createdAt), sort = OrderFilter.Sort.EARLIEST_FIRST
        )

        shouldReadAllByOne(
            { cont -> orderService.getSellOrdersByItemAndStatus(
                ItemId("A", 5), null, "A.5.Flow", emptyList(), cont, 1, OrderFilter.Sort.EARLIEST_FIRST
            ).toList() },
            2, 0, cmp = Comparator.comparing(Order::createdAt), sort = OrderFilter.Sort.EARLIEST_FIRST
        )

        shouldReadAllByOne(
            { cont -> orderService.getSellOrdersByItemAndStatus(
                ItemId("A", 1), null, "A.1.Flow", listOf(OrderStatus.ACTIVE), cont, 1, OrderFilter.Sort.EARLIEST_FIRST
            ).toList() },
            2, 0, cmp = Comparator.comparing(Order::createdAt), sort = OrderFilter.Sort.EARLIEST_FIRST
        )
    }

    suspend fun shouldReadAllByOne(
        fn: suspend (continuation: String?) -> List<Order>,
        expectedCount: Int,
        currentIteration: Int = 0,
        continuation: String? = null,
        last: Order? = null,
        cmp: Comparator<Order>? = null,
        sort: OrderFilter.Sort = OrderFilter.Sort.LATEST_FIRST
    ) {
        val result = fn(continuation)
        if(result.isEmpty()) {
            currentIteration shouldBe expectedCount
        } else {
            if (last != null && cmp != null) {
                listOf(last, result[0]) shouldBeSortedWith cmp
            }

            val cont = sort.nextPage(result[0])
            shouldReadAllByOne(fn, expectedCount, currentIteration + 1, cont, result[0], cmp, sort)
        }
    }

    fun createOrder(id: Long = randomLong()) = Order(
        id = id,
        itemId = ItemId(FlowAddress("0x01").formatted, 1),
        maker = FlowAddress("0x1000"),
        taker = null,
        make = FlowAssetNFT("0x01", 1.toBigDecimal(), 1),
        take = FlowAssetFungible("FLOW", BigDecimal.TEN),
        amount = BigDecimal.TEN,
        type = OrderType.LIST,
        data = OrderData(emptyList(), emptyList()),
        collection = "ABC",
        fill = 13.37.toBigDecimal(),
        lastUpdatedAt = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        makeStock = BigInteger.TEN
    )
}
