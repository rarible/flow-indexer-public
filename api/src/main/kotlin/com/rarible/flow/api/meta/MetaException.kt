package com.rarible.flow.api.meta

import com.rarible.protocol.dto.FlowMetaDto

class MetaException(
    override val message: String,
    val status: Status
) : Exception(message) {

    // TODO ideally there should be separate mapper to DTO
    enum class Status(private val dto: FlowMetaDto.Status) {

        NOT_FOUND(FlowMetaDto.Status.NOT_FOUND),
        TIMEOUT(FlowMetaDto.Status.TIMEOUT),
        CORRUPTED_URL(FlowMetaDto.Status.CORRUPTED_URL),
        CORRUPTED_DATA(FlowMetaDto.Status.CORRUPTED_DATA),
        ERROR(FlowMetaDto.Status.ERROR);

        fun toDto(): FlowMetaDto.Status {
            return this.dto
        }
    }
}