package com.rarible.flow.scanner.eventlisteners

import com.google.protobuf.UnsafeByteOperations
import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.impl.AsyncFlowAccessApiImpl
import com.rarible.flow.core.domain.ItemId
import com.rarible.flow.core.domain.ItemMeta
import com.rarible.flow.core.domain.Ownership
import com.rarible.flow.core.domain.QOwnership
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.scanner.model.MotoGPCardMetadata
import com.rarible.flow.scanner.model.MotoGPCardNFT
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitFirst
import net.devh.boot.grpc.client.inject.GrpcClient
import org.onflow.protobuf.access.AccessAPIGrpc
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import javax.annotation.PostConstruct

@Component
class MotoGPCardMetaExtractor(
    @Value("\${blockchain.scanner.flow.chainId}")
    private val chainId: FlowChainId,
    private val ownershipRepository: OwnershipRepository
) : NftMetaExtractor {

    private val contract = "MotoGPCard"

    private val motoGPCardScript = """
        import NonFungibleToken from 0x1d7e57aa55817448
        import MotoGPCard from 0xa49cc0ee46c54bfb

        pub fun main(address: Address, tokenId: UInt64): &AnyResource {
            let account = getAccount(address)
            let collection = getAccount(address).getCapability<&{MotoGPCard.ICardCollectionPublic}>(/public/motogpCardCollection).borrow()!
            return collection.borrowNFT(id: tokenId)
        }
    """.trimIndent()

    private val motoGPMetadataScript = """
        import NonFungibleToken from 0x1d7e57aa55817448
        import MotoGPCard from 0xa49cc0ee46c54bfb
        import MotoGPCardMetadata from 0xa49cc0ee46c54bfb

        pub fun main(address: Address, tokenId: UInt64): MotoGPCardMetadata.Metadata? {
            let account = getAccount(address)
            let collection = getAccount(address).getCapability<&{MotoGPCard.ICardCollectionPublic}>(/public/motogpCardCollection).borrow()!
            let ref = collection.borrowCard(id: tokenId)!
            return ref.getCardMetadata()
        }
    """.trimIndent()

    @GrpcClient("flow")
    private lateinit var stub: AccessAPIGrpc.AccessAPIFutureStub

    private lateinit var api: AsyncFlowAccessApi

    private val cadenceBuilder = JsonCadenceBuilder()

    private val addressRegistry = Flow.DEFAULT_ADDRESS_REGISTRY

    @PostConstruct
    fun postCreate() {
        api = AsyncFlowAccessApiImpl(stub)
    }

    override suspend fun supported(contractName: String): Boolean =
        contractName.contains(contract, true)

    override suspend fun extract(itemId: ItemId): ItemMeta? {
        val lastKnownOwner = lastKnownOwnership(itemId) ?: return null

        val card = cardData(itemId.tokenId, lastKnownOwner.owner)
        val metadata = meta(itemId.tokenId, lastKnownOwner.owner)
        return ItemMeta(
            itemId = itemId,
            title = metadata.name,
            description = metadata.description,
            uri = URI(metadata.imageUrl),
            properties = metadata.data + mapOf(
                "uuid" to card.uuid,
                "serial" to card.serial,
                "cardId" to card.cardID
            )
        )
    }

    private suspend fun lastKnownOwnership(id: ItemId): Ownership? {
        val q = QOwnership.ownership
        val predicate = q.contract.eq(id.contract).and(q.tokenId.eq(id.tokenId))
        return ownershipRepository.findAll(predicate, q.date.desc()).awaitFirst()
    }

    private suspend fun cardData(tokenId: Long, owner: FlowAddress): MotoGPCardNFT {
        val builder = ScriptBuilder()
        builder.script(addressRegistry.processScript(motoGPCardScript, chainId))
        builder.arguments(
            mutableListOf(
                cadenceBuilder.address(owner.formatted),
                cadenceBuilder.number("UInt64", tokenId)
            )
        )
        val response = api.executeScriptAtLatestBlock(
            script = builder.script,
            arguments = builder.arguments.map { UnsafeByteOperations.unsafeWrap(Flow.encodeJsonCadence(it)) }
        ).await()
        return Flow.unmarshall(MotoGPCardNFT::class, response.jsonCadence)
    }

    private suspend fun meta(tokenId: Long, owner: FlowAddress): MotoGPCardMetadata {
        val builder = ScriptBuilder()
        builder.script(addressRegistry.processScript(motoGPMetadataScript, chainId))
        builder.arguments(
            mutableListOf(
                cadenceBuilder.address(owner.formatted),
                cadenceBuilder.number("UInt64", tokenId)
            )
        )
        val response = api.executeScriptAtLatestBlock(
            script = builder.script,
            arguments = builder.arguments.map { UnsafeByteOperations.unsafeWrap(Flow.encodeJsonCadence(it)) }
        ).await()
        return Flow.unmarshall(MotoGPCardMetadata::class, response.jsonCadence)

    }
}
