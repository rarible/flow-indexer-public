package com.rarible.flow.scanner.migrations

import com.rarible.flow.Contracts
import com.rarible.flow.core.domain.Item
import com.rarible.flow.scanner.config.FlowApiProperties
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.updateMulti

@ChangeUnit(
    id = "ChangeLog00024MotoGpRoyalties",
    order = "00024",
    author = "flow"
)
class ChangeLog00024MotoGpRoyalties(
    private val config: FlowApiProperties,
    private val mongo: MongoTemplate
) {

    @Execution
    fun changeSet() {
        mongo.updateMulti<Item>(
            Query(
                Item::contract isEqualTo Contracts.MOTOGP.fqn(config.chainId)
            ),
            Update.update(Item::royalties.name, Contracts.MOTOGP.staticRoyalties(config.chainId))
        )
    }

    @RollbackExecution
    fun rollBack() {

    }
}
