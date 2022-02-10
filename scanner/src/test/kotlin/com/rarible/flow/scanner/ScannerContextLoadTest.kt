package com.rarible.flow.scanner

import com.rarible.flow.log.Log
import com.rarible.flow.scanner.listener.ActivityMaker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


@IntegrationTest
class ScannerContextLoadTest: BaseIntegrationTest() {

    @Autowired
    lateinit var activityMakers: List<ActivityMaker>

    @Test
    fun contextLoads() {}

    @Test
    fun `should load all activity makers`() {
        logger.info("Activity Makers: {}", activityMakers.map { it.javaClass.simpleName }.sorted() )
        activityMakers.size shouldBe 13
    }

    companion object {
        val logger by Log()
    }
}
