package com.rarible.flow.api.service

import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.OwnershipContinuation
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import org.onflow.sdk.FlowAddress
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink

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
            val cont = OwnershipContinuation.of(continuation)
            var flux = if (cont != null) {
               ownershipRepository.findAllByDateAfterAndIdNotOrderByDateDesc(cont.afterDate, cont.afterId)
            } else {
               ownershipRepository.findAll()
            }

            if (size != null) {
                flux = flux.take(size.toLong(), true)
            }

            flux.collectList().subscribe {
                doAnswer(
                    items = it,
                    sink = sink
                )
            }
        }

    suspend fun byItem(
        contract: FlowAddress,
        tokenId: TokenId,
        continuation: String?,
        size: Int?
    ): Mono<FlowNftOwnershipsDto> =
        Mono.create { sink ->
            val cont = OwnershipContinuation.of(continuation)
            var flux = if (cont != null) {
                    ownershipRepository.findAllByContractAndTokenIdAndDateAfterAndIdNotOrderByDateDesc(
                        contract,
                        tokenId,
                        cont.afterDate,
                        cont.afterId
                    )
            } else {
                ownershipRepository.findAllByContractAndTokenIdOrderByDateDesc(contract, tokenId)
            }

            if (size != null) {
                flux = flux.take(size.toLong(), true)
            }

            flux.collectList().subscribe {
                doAnswer(
                    items = it,
                    sink = sink
                )
            }
        }

    private fun doAnswer(
        items: List<Ownership>,
        sink: MonoSink<FlowNftOwnershipsDto>
    ) {
        val answerContinuation = OwnershipContinuation(items.last().date, items.last().id)
        sink.success(
            FlowNftOwnershipsDto(
                total = items.size,
                continuation = "$answerContinuation",
                ownerships = items.map(OwnershipToDtoConverter::convert)
            )
        )
    }

}
