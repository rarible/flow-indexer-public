package com.rarible.flow.scanner.batch

import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.protobuf.entities.BlockOuterClass
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.listener.ExecutionContextPromotionListener
import org.springframework.batch.core.listener.JobParameterExecutionContextCopyListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Created by TimochkinEA at 10.06.2021
 */
@Configuration
@EnableBatchProcessing
class BatchConfig {

    @Autowired
    private lateinit var jobBuilderFactory: JobBuilderFactory

    @Autowired
    private lateinit var stepBuilderFactory: StepBuilderFactory

    @GrpcClient("flow")
    private lateinit var client: AccessAPIGrpc.AccessAPIBlockingStub

    @Bean
    fun reader(blockRepository: FlowBlockRepository): FlowReader {
        val lastBlockInDB = blockRepository.findTopByOrderByHeightDesc().block()
        val lastBlockOnChain = client.getLatestBlockHeader(Access.GetLatestBlockHeaderRequest.newBuilder().setIsSealed(true).build()).block

        val start = if (lastBlockInDB != null) {
            val ld = lastBlockInDB.timestamp
            val bld = Instant.ofEpochSecond(lastBlockOnChain.timestamp.seconds)
            val diff = ChronoUnit.SECONDS.between(ld, bld)
            if (diff > 60L)  {
                lastBlockOnChain.height - 1
            } else {
                lastBlockInDB.height
            }
        } else {
            lastBlockOnChain.height - 1
        }
        return FlowReader(client, start)
    }

    @Bean
    fun processor(): FlowProcessor = FlowProcessor(client)

    @Bean
    fun writer(blockRepository: FlowBlockRepository, txRepository: FlowTransactionRepository): FlowWriter = FlowWriter(blockRepository, txRepository)

    @Bean
    fun readJob(readStep: Step): Job {
        return jobBuilderFactory.get("readJob")
            .incrementer(RunIdIncrementer())
            .flow(readStep)
            .end()
            .build()
    }

    @Bean
    fun copyListener(): JobParameterExecutionContextCopyListener {
        val l = JobParameterExecutionContextCopyListener()
        l.setKeys(arrayOf("latest.height"))
        return l
    }

    @Bean
    fun readStep(blockRepository: FlowBlockRepository, txRepository: FlowTransactionRepository): Step {
        return stepBuilderFactory.get("readStep")
            .chunk<BlockOuterClass.Block, Pair<FlowBlock, List<FlowTransaction>>>(1)
            .reader(reader(blockRepository))
            .processor(processor())
            .writer(writer(blockRepository, txRepository))
            .faultTolerant()
            .retry(StatusRuntimeException::class.java)
            .retry(StatusException::class.java)
            .retryLimit(5)
            .listener(ExecutionContextPromotionListener().apply { setKeys(arrayOf("latest.height")) })
            .build()
    }
}
