package com.rarible.flow.api.service

import com.rarible.flow.core.converter.OwnershipToDtoConverter
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.protocol.dto.FlowNftOwnershipDto
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
}
