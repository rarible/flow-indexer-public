package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.Ownership
import com.rarible.protocol.dto.FlowNftOwnershipDto
import org.springframework.core.convert.converter.Converter

object OwnershipToDtoConverter : Converter<Ownership, FlowNftOwnershipDto> {

    override fun convert(ownership: Ownership): FlowNftOwnershipDto {
        return FlowNftOwnershipDto(
            ownership.id.toString(),
            ownership.contract.formatted,
            ownership.tokenId.toString(),
            ownership.owner.formatted,
        )
    }
}