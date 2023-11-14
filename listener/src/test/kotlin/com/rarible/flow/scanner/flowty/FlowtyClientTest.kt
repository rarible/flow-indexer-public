package com.rarible.flow.scanner.flowty

import com.rarible.flow.scanner.config.FlowListenerProperties
import com.rarible.flow.scanner.config.FlowtyProperties
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FlowtyClientTest {

    @Test
    @Disabled
    fun `get token - ok`() = runBlocking<Unit> {
        val properties = mockk< FlowListenerProperties> {
            every { flowty } returns FlowtyProperties()
        }
        val client = FlowtyClient(properties)
        val token = client.getGamisodesToken(0)
        assertThat(token.owner).isNotBlank()
    }
}
