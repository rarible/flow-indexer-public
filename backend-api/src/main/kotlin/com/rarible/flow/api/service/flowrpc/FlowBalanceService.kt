package com.rarible.flow.api.service.flowrpc

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.ArrayField
import com.nftco.flow.sdk.cadence.Field
import com.rarible.flow.api.simpleScript
import com.rarible.flow.core.domain.Balance


class FlowBalanceService(
    private val chainId: FlowChainId,
    private val flowAccessApi: AsyncFlowAccessApi
) {
    private val script = this.javaClass.getResource("/script/fungible_balances.cdc").readText()

    suspend fun initBalances(accounts: Set<FlowAddress>): List<Balance> {
        return executeScript(script, accounts)
    }

    /**
     *
     */
    suspend fun initBalances(account: FlowAddress, token: String): Balance? {
        return executeScript(script, setOf(account)).firstOrNull {
            it.account == account && it.token == token
        }
    }

    suspend fun executeScript(scriptText: String, accounts: Set<FlowAddress>): List<Balance> {
        return convert(
            flowAccessApi.simpleScript {
                script(
                    scriptText, chainId
                )

                arg {
                    array {
                        accounts.map { acc -> address(acc.bytes) }
                    }
                }

            }.jsonCadence
        )
    }

    fun convert(jsonCadence: Field<*>): List<Balance> {
        jsonCadence as ArrayField
        return jsonCadence.value?.map {
            Flow.unmarshall(BalanceC::class, it).toDomain()
        } ?: emptyList()
    }
}