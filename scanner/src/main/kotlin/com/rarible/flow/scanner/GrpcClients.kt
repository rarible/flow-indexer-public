package com.rarible.flow.scanner

import org.springframework.stereotype.Component

/**
 * Created by TimochkinEA at 30.05.2021
 *
 * Manage gRPC clients for Flow Access API
 */
object GrpcClients {

    private val clients = listOf(
        GrpcClient("static://access-001.mainnet1.nodes.onflow.org:9000", 7601063L..8742958L),
        GrpcClient("static://access-001.mainnet2.nodes.onflow.org:9000", 8742959L..9737132L),
        GrpcClient("static://access-001.mainnet3.nodes.onflow.org:9000", 9737133L..9992019L),
        GrpcClient("static://access-001.mainnet4.nodes.onflow.org:9000", 9992020L..12020336L),
        GrpcClient("static://access-001.mainnet5.nodes.onflow.org:9000", 12020337L..12609236L),
        GrpcClient("static://access.mainnet.nodes.onflow.org:9000", 13404174L..Long.MAX_VALUE),
    )

    fun forBlock(blockHeight: Long): GrpcClient = clients.first { try {it.blocksRange.contains(blockHeight)} catch (e: Throwable) {false} }

}


