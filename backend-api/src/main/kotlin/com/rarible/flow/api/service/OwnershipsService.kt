package com.rarible.flow.api.service

import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OwnershipContinuation
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.core.repository.coFindById
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

@Service
class OwnershipsService(
    private val ownershipRepository: OwnershipRepository
) {

    suspend fun byId(id: String): FlowNftOwnershipDto? {
        val ownership = ownershipRepository.coFindById(OwnershipId.parse(id))
        return if (ownership == null) null else OwnershipToDtoConverter.convert(ownership)
    }

    suspend fun all(continuation: String?, size: Int?): FlowNftOwnershipsDto {
        val flow = ownershipRepository.all(continuation, size).asFlow()
        return flowNftOwnershipsDto(flow, size)
    }

    suspend fun byItem(
        contract: String,
        tokenId: TokenId,
        continuation: String?,
        size: Int?
    ): FlowNftOwnershipsDto {
        val flow = ownershipRepository.byItem(contract, tokenId, continuation, size).asFlow()
        return flowNftOwnershipsDto(flow, size)
    }

    private suspend fun flowNftOwnershipsDto(
        flow: Flow<Ownership>,
        size: Int?
    ): FlowNftOwnershipsDto {
        var result = flow
        if (size != null) {
            result = result.take(size)
        }

        val items = result.toList()

        return FlowNftOwnershipsDto(
            total = items.size.toLong(),
            ownerships = items.map(OwnershipToDtoConverter::convert),
            continuation = answerContinuation(items)
        )
    }

    private fun answerContinuation(items: List<Ownership>): String? = if (items.isEmpty()) null else "${
        OwnershipContinuation(
            beforeDate = items.last().date,
            beforeId = items.last().id
        )
    }"
}
