package com.rarible.flow.scanner.service.balance

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowChainId
import com.nftco.flow.sdk.cadence.ArrayField
import com.nftco.flow.sdk.cadence.Field
import com.rarible.blockchain.scanner.flow.service.AsyncFlowAccessApi
import com.rarible.flow.core.domain.Balance
import com.rarible.flow.core.repository.BalanceRepository
import com.rarible.flow.core.repository.coSave
import com.rarible.flow.sdk.simpleScript


class FlowBalanceService(
    private val chainId: FlowChainId,
    private val flowAccessApi: AsyncFlowAccessApi,
    private val balanceRepository: BalanceRepository
) {
    private val script = this.javaClass.getResource("/scripts/fungible_balances.cdc").readText()

    /**
     * Reads current balance from the blockchain and saves it for future tracking
     */
    suspend fun initBalances(account: FlowAddress, token: String): Balance? {
        return executeScript(script, setOf(account)).firstOrNull {
            it.account == account && it.token == token
        }?.let {
            balanceRepository.coSave(it)
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



    companion object {
        fun convert(jsonCadence: Field<*>): List<Balance> {
            jsonCadence as ArrayField
            return jsonCadence.value?.map {
                Flow.unmarshall(BalanceC::class, it).toDomain()
            } ?: emptyList()
        }
    }
}