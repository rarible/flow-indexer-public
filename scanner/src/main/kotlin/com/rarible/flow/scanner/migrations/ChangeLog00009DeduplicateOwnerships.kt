package com.rarible.flow.scanner.migrations

import com.mongodb.MongoNamespace
import com.rarible.flow.log.Log
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * {
"aggregate": "item",
"pipeline": [{
"$project": {
"_id": {
"$concat": [
"$_id", ":", "$owner"
]
},
"contract": "$contract",
"tokenId": "$tokenId",
"owner": "$owner",
"creator": "$creator",
"date": "$mintedAt",
"_class": "com.rarible.flow.core.domain.Ownership"
}
}, {
"$out": "ownership_fix"
}],
"cursor": {}
}
 */

@ChangeUnit(
    id = "ChangeLog00009DeduplicateOwnerships",
    order = "00009",
    author = "flow"
)
class ChangeLog00009DeduplicateOwnerships(
    private val mongoTemplate: MongoTemplate
) {

    @Execution
    fun changeSet() {
        mongoTemplate.getCollection("ownership").renameCollection(
            MongoNamespace(mongoTemplate.db.name, "ownership_old")
        )

        val result = mongoTemplate.executeCommand(migrationCommand)

        log.info("Command result: {}", result)

        mongoTemplate.getCollection(NEW_COLLECTION).renameCollection(
            MongoNamespace(mongoTemplate.db.name, OWNERSHIP)
        )


    }

    @RollbackExecution
    fun rollBack() {
        mongoTemplate.dropCollection(OWNERSHIP)
        mongoTemplate.getCollection(OLD_COLLECTION).renameCollection(
            MongoNamespace(mongoTemplate.db.name, OWNERSHIP)
        )
    }

    companion object {
        private const val OLD_COLLECTION = "ownership_old"
        private const val NEW_COLLECTION = "ownership_fix"
        private const val OWNERSHIP = "ownership"

        const val migrationCommand = "{" +
                "    \"aggregate\": \"item\"," +
                "    \"pipeline\": [{" +
                "        \"\$project\": {" +
                "            \"_id\": {" +
                "                \"\$concat\": [" +
                "                    \"\$_id\", \":\", \"\$owner\"" +
                "                ]" +
                "            }," +
                "            \"contract\": \"\$contract\"," +
                "            \"tokenId\": \"\$tokenId\"," +
                "            \"owner\": \"\$owner\"," +
                "            \"creator\": \"\$creator\"," +
                "            \"date\": \"\$mintedAt\"," +
                "            \"_class\": \"com.rarible.flow.core.domain.Ownership\"" +
                "        }" +
                "    }, {" +
                "        \"\$out\": \"ownership_fix\"" +
                "    }]," +
                "    \"cursor\": {}" +
                "}"

        private val log by Log()


    }
}
