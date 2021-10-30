package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.Ownership
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.PayInfoDto
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal

object OwnershipToDtoConverter : Converter<Ownership, FlowNftOwnershipDto> {

    override fun convert(ownership: Ownership): FlowNftOwnershipDto {
        return FlowNftOwnershipDto(
            id = ownership.id.toString(),
            contract = ownership.contract,
            tokenId = ownership.tokenId.toBigInteger(),
            owner = ownership.owner.formatted,
            creators = listOf(
                PayInfoDto(
                    ownership.creator.formatted, BigDecimal.ONE
                )
            ),
            createdAt = ownership.date
        )
    }
}
