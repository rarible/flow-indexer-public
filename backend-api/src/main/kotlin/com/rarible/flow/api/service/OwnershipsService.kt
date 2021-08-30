package com.rarible.flow.api.service

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.QOwnership
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
        val predicate = BooleanBuilder(byContinuation(OwnershipContinuation.of(continuation)))
        val flow = ownershipRepository.findAll(predicate, *defaultOrder()).asFlow()
        return flowNftOwnershipsDto(flow, size)
    }

    suspend fun byItem(
        contract: String,
        tokenId: TokenId,
        continuation: String?,
        size: Int?
    ): FlowNftOwnershipsDto {
        val predicate = BooleanBuilder(byContinuation(OwnershipContinuation.of(continuation)))
        predicate.and(byContractAndTokenId(contract, tokenId))
        val flow = ownershipRepository.findAll(predicate).asFlow()
        return flowNftOwnershipsDto(flow, size)
    }

    private fun defaultOrder(): Array<OrderSpecifier<*>> = arrayOf(
        QOwnership.ownership.date.desc(),
        QOwnership.ownership.id.contract.desc(),
        QOwnership.ownership.id.tokenId.desc()
    )

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
            total = items.size,
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

    private fun byContractAndTokenId(contract: String, tokenId: TokenId): BooleanBuilder {
        val q = QOwnership.ownership

        return BooleanBuilder(q.contract.eq(contract)).and(q.tokenId.eq(tokenId))
    }

    private fun byContinuation(cont: OwnershipContinuation?): BooleanBuilder {
        val q = QOwnership.ownership
        return if (cont == null) BooleanBuilder() else BooleanBuilder(q.date.before(cont.beforeDate)).and(q.id.ne(cont.beforeId))
    }
}
