package com.rarible.flow.scanner.batch

import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.protobuf.entities.BlockOuterClass
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.support.AbstractItemStreamItemReader

/**
 * Created by TimochkinEA at 09.06.2021
 */
class FlowReader(private val grpcClient: AccessAPIGrpc.AccessAPIBlockingStub, private var latestHeight: Long = 0L): AbstractItemStreamItemReader<BlockOuterClass.Block>() {

    private val LATEST_HEIGHT = "latest.height"

    override fun read(): BlockOuterClass.Block? {
        return try {
            val req = Access.GetBlockByHeightRequest.newBuilder()
                .setHeight(latestHeight)
                .build()

            val b = grpcClient.getBlockByHeight(req).block
            latestHeight++
            b
        } catch (e: StatusRuntimeException) {
            null
        } catch (e: StatusException) {
            null
        }
    }

    override fun update(executionContext: ExecutionContext) {
        super.update(executionContext)
        executionContext.putLong(LATEST_HEIGHT, latestHeight)
    }
}
