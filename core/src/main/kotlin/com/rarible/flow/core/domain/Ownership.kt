package com.rarible.flow.core.domain

import com.nftco.flow.sdk.FlowAddress
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.Clock
import java.time.Instant

@Document(Ownership.COLLECTION)
data class Ownership(
    val contract: String,
    val tokenId: TokenId,
    val owner: FlowAddress,
    val creator: FlowAddress,
    val date: Instant = Instant.now(Clock.systemUTC()),
    @Version
    val version: Long? = null
) : Serializable {

    constructor(
        id: OwnershipId,
        creator: FlowAddress,
        date: Instant = Instant.now(Clock.systemUTC())
    ) : this(id.contract, id.tokenId, id.owner, creator, date)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: OwnershipId
        get() = OwnershipId(owner = owner, contract = contract, tokenId = tokenId)
        set(_) {}

    fun transfer(to: FlowAddress): Ownership = this.copy(owner = to)

    companion object {

        const val COLLECTION = "ownership"
    }
}
