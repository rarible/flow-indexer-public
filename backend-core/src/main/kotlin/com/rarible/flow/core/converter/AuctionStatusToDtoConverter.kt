package com.rarible.flow.core.converter

import com.rarible.flow.core.domain.AuctionStatus
import com.rarible.protocol.dto.FlowAuctionStatusDto
import org.springframework.core.convert.converter.Converter

object AuctionStatusToDtoConverter: Converter<AuctionStatus, FlowAuctionStatusDto> {
    override fun convert(source: AuctionStatus): FlowAuctionStatusDto {
        return when(source) {
            AuctionStatus.INACTIVE -> FlowAuctionStatusDto.INACTIVE
            AuctionStatus.ACTIVE -> FlowAuctionStatusDto.ACTIVE
            AuctionStatus.CANCELLED -> FlowAuctionStatusDto.CANCELLED
            AuctionStatus.FINISHED -> FlowAuctionStatusDto.FINISHED
        }
    }
}
