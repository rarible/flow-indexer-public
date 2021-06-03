package com.rarible.flow.scanner.repo

import com.rarible.flow.scanner.model.FlowBlock
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * Created by TimochkinEA at 22.05.2021
 */
@Repository
interface FlowBlockRepository: MongoRepository<FlowBlock, String> {

    fun findTopByOrderByHeightDesc(): FlowBlock?

    fun findTopByOrderByHeightAsc(): FlowBlock?
}
