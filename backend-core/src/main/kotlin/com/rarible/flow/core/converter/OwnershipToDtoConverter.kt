package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.Ownership
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.PayInfoDto
import org.springframework.core.convert.converter.Converter
import java.time.Instant

object OwnershipToDtoConverter : Converter<Ownership, FlowNftOwnershipDto> {

    override fun convert(ownership: Ownership): FlowNftOwnershipDto {
        return FlowNftOwnershipDto(
            id = ownership.id.toString(),
            contract = ownership.contract,
            tokenId = ownership.tokenId,
            owner = ownership.owner.formatted,
            creators = ownership.creators.map { PayInfoDto(account = it.account.formatted, value = it.value) },
            createdAt = ownership.date
        )
    }
}
