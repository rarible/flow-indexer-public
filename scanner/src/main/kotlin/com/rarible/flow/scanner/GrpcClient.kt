package com.rarible.flow.scanner

import io.grpc.ManagedChannelBuilder
import org.onflow.protobuf.access.AccessAPIGrpc

/**
 * Created by TimochkinEA at 31.05.2021
 */
class GrpcClient(address: String, val blocksRange: LongRange) {

    val sync: AccessAPIGrpc.AccessAPIBlockingStub

    val async: AccessAPIGrpc.AccessAPIStub

    val future: AccessAPIGrpc.AccessAPIFutureStub

    init {
        val channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build()
        sync = AccessAPIGrpc.newBlockingStub(channel)
        async = AccessAPIGrpc.newStub(channel)
        future = AccessAPIGrpc.newFutureStub(channel)
    }
}
