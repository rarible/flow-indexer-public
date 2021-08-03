package com.rarible.flow.api.service

import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OwnershipContinuation
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class OwnershipsService(
    private val ownershipRepository: OwnershipRepository
) {

    suspend fun byId(id: String): Mono<FlowNftOwnershipDto> =
        Mono.create { sink ->
            ownershipRepository.findById(OwnershipId.parse(id)).subscribe {
                sink.success(OwnershipToDtoConverter.convert(it))
            }
        }

    suspend fun all(continuation: String?, size: Int?): Mono<FlowNftOwnershipsDto> =
        Mono.create { sink ->
            val flux = if (continuation != null) {
                ownershipRepository.findAllByDateAfter(OwnershipContinuation(continuation).afterDate)
            } else {
                ownershipRepository.findAll()
            }
            flux.collectList().subscribe {
                val items = if (size != null) {
                    it.take(size)
                } else it
                sink.success(
                    FlowNftOwnershipsDto(
                        total = size ?: it.size,
                        continuation = continuation,
                        ownerships = items.map(OwnershipToDtoConverter::convert)
                    )
                )
            }
        }


    suspend fun byItem(contract: FlowAddress, tokenId: TokenId, continuation: String?, size: Int?): Mono<FlowNftOwnershipsDto> =
        Mono.create { sink ->
            val flux = if (continuation != null) {
                ownershipRepository.findAllByContractAndTokenIdAndDateAfter(contract, tokenId, OwnershipContinuation(continuation).afterDate)
            } else {
                ownershipRepository.findAllByContractAndTokenId(contract, tokenId)
            }

            flux.collectList().subscribe {
                val items = if (size != null) {
                    it.take(size)
                } else it
                sink.success(
                    FlowNftOwnershipsDto(
                        total = size ?: it.size,
                        continuation = continuation,
                        ownerships = items.map(OwnershipToDtoConverter::convert)
                    )
                )
            }
        }

}
