package com.rarible.flow.core.service

import com.nftco.flow.sdk.FlowChainId
import com.rarible.blockchain.scanner.flow.service.Spork
import com.rarible.blockchain.scanner.flow.service.SporkService
import com.rarible.flow.core.config.AppProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SporkConfigurationService(
    private val sporkService: SporkService,
    private val properties: AppProperties
) {
    suspend fun config() {
        val chainId = properties.chainId
        logger.info("Config: chainId=$chainId, accessUrl=${properties.flowAccessUrl}")

        if (chainId == FlowChainId.EMULATOR) {
            sporkService.replace(
                FlowChainId.EMULATOR, listOf(
                Spork(
                    from = 0,
                    to = Long.MAX_VALUE,
                    nodeUrl = properties.flowAccessUrl,
                    port = properties.flowAccessPort,
                )
            ))
        }
        if (chainId == FlowChainId.TESTNET) {
            sporkService.replace(
                FlowChainId.TESTNET, listOf(
                Spork(
                    from = 105032150,
                    nodeUrl = properties.flowAccessUrl,
                    port = properties.flowAccessPort,
                )
            ))
        }
        if (chainId == FlowChainId.MAINNET) {
            val head = listOf(
                Spork(
                    from = 55114467,
                    nodeUrl = properties.flowAccessUrl,
                    port = properties.flowAccessPort,
                ),
                Spork(
                    to = 55114466,
                    from = 47169687,
                    nodeUrl = "access-001.mainnet22.nodes.onflow.org",
                ),
                Spork(
                    to = 47169686,
                    from = 44950207,
                    nodeUrl = "access-001.mainnet21.nodes.onflow.org",
                ),
                Spork(
                    to = 44950206,
                    from = 40171634,
                    nodeUrl = "access-001.mainnet20.nodes.onflow.org",
                ),
                Spork(
                    to = 40171633L,
                    from = 35858811L,
                    nodeUrl = "access-001.mainnet19.nodes.onflow.org",
                ),
                Spork(
                    from = 35858811L,
                    to = 40171633L,
                    nodeUrl = "access-001.mainnet19.nodes.onflow.org",
                ),
                Spork(
                    from = 31735955L,
                    to = 35858810L,
                    nodeUrl = "access-001.mainnet18.nodes.onflow.org",
                ),
                Spork(
                    from = 27341470L,
                    to = 31735954L,
                    nodeUrl = "access-001.mainnet17.nodes.onflow.org",
                ),
                Spork(
                    from = 23830813L,
                    to = 27341469L,
                    nodeUrl = "access-001.mainnet16.nodes.onflow.org",
                ),
                Spork(
                    from = 21291692L,
                    to = 23830812L,
                    nodeUrl = "access-001.mainnet15.nodes.onflow.org"
                ),
                Spork(
                    from = 19050753L,
                    to = 21291691L,
                    nodeUrl = "access-001.mainnet14.nodes.onflow.org"
                ),
            )
            val tail = sporkService.sporks(FlowChainId.MAINNET).drop(1)
            sporkService.replace(FlowChainId.MAINNET, head + tail)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SporkConfigurationService::class.java)
    }
}