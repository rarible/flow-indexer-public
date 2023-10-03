package com.rarible.flow.scanner

import com.nftco.flow.sdk.Signer
import com.nftco.flow.sdk.cadence.JsonCadenceBuilder
import com.nftco.flow.sdk.crypto.Crypto
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.flow.core.domain.Item
import com.rarible.flow.core.domain.OwnershipId
import com.rarible.flow.core.domain.TokenId
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OwnershipRepository
import com.rarible.flow.scanner.test.AbstractIntegrationTest
import com.rarible.flow.scanner.test.EmulatorUser
import com.rarible.flow.scanner.test.FlowTestContainer
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RaribleScannerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    private val cb = JsonCadenceBuilder()

    @Test
    fun `mint item`() = runBlocking<Unit> {
        val minter = EmulatorUser.Emulator
        val meta = randomString()

        mint(meta, minter)

        Wait.waitAssert {
            val minted = findItem(meta)
            assertThat(minted).isNotNull()
            assertThat(minted!!.owner).isEqualTo(minter.address)

            val ownershipId = OwnershipId(minted.contract, minted.tokenId, minter.address)
            val ownership = ownershipRepository.findById(ownershipId).awaitSingleOrNull()
            assertThat(ownership).isNotNull()

            assertThat(findItemUpdates(minted.id.toString())).hasSize(1)
            assertThat(findOwnershipUpdates(ownershipId.toString())).hasSize(1)
        }
    }

    @Test
    fun `burn item`() = runBlocking<Unit> {
        val minter = EmulatorUser.Emulator
        val meta = randomString()

        mint(meta, minter)
        Wait.waitAssert { assertThat(findItem(meta)).isNotNull() }

        val item = findItem(meta)!!

        burn(item.tokenId, minter)

        Wait.waitAssert {
            val burned = findItem(meta)
            assertThat(burned).isNotNull()
            assertThat(burned!!.owner).isNull()

            val ownershipId = OwnershipId(burned.contract, burned.tokenId, minter.address)
            val ownership = ownershipRepository.findById(ownershipId).awaitSingleOrNull()
            assertThat(ownership).isNull()

            assertThat(findItemUpdates(burned.id.toString())).hasSize(1)
            assertThat(findOwnershipUpdates(ownershipId.toString())).hasSize(1)

            assertThat(findItemDeletions(burned.id.toString())).hasSize(1)
            assertThat(findOwnershipDeletions(ownershipId.toString())).hasSize(1)
        }
    }

    private suspend fun mint(meta: String, user: EmulatorUser) {
        FlowTestContainer.execute(
            address = user.address,
            signer = getSigner(user),
            scriptPath = "/emulator/transactions/rarible_nft_mint.cdc",
            cb.string(meta),
            cb.array { emptyList() }
        )
    }

    private suspend fun burn(tokenId: TokenId, user: EmulatorUser) {
        FlowTestContainer.execute(
            address = user.address,
            signer = getSigner(user),
            scriptPath = "/emulator/transactions/rarible_nft_burn.cdc",
            cb.uint64(tokenId)
        )
    }

    private suspend fun findItem(meta: String): Item? {
        return itemRepository.findAll()
            .filter { it.meta == """{"metaURI":"$meta"}""" }
            .collectList()
            .awaitSingle()
            .firstOrNull()
    }

    private fun getSigner(user: EmulatorUser): Signer {
        val payerKey = FlowTestContainer.getAccountKey(user.address)
        return Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(user.keyHex),
            hashAlgo = payerKey.hashAlgo
        )
    }
}
