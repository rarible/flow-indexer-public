import NonFungibleToken from "flow/NonFungibleToken.cdc"
import MetadataViews from "flow/MetadataViews.cdc"
import LicensedNFT from "LicensedNFT.cdc"

pub contract RaribleNFTv2 : NonFungibleToken, LicensedNFT {

    pub var totalSupply: UInt64

    pub let CollectionPublicPath: PublicPath
    pub let CollectionStoragePath: StoragePath

    pub event ContractInitialized()
    pub event Withdraw(id: UInt64, from: Address?)
    pub event Deposit(id: UInt64, to: Address?)
    pub event Minted(id: UInt64, meta: Meta, creator: Address, royalties: [LicensedNFT.Royalty])
    pub event Burned(id: UInt64)

    pub struct Royalty {
        pub let address: Address
        pub let fee: UFix64

        init(address: Address, fee: UFix64) {
            self.address = address
            self.fee = fee
        }
    }

    pub struct Meta {
        pub let name: String
        pub let description: String?
        pub let cid: String
        pub let attributes: {String:String}
        pub let contentUrls: [String]

        init (name: String, description: String, cid: String, attributes: {String:String}, contentUrls: [String]) {
            self.name = name
            self.description = description
            self.cid = cid
            self.attributes = attributes
            self.contentUrls = contentUrls
        }
    }

    pub resource NFT: NonFungibleToken.INFT {
        pub let id: UInt64
        pub let parentId: UInt64
        pub let creator: Address
        access(self) let meta: Meta
        access(self) let royalties: [LicensedNFT.Royalty]

        init (id: UInt64, parentId: UInt64, creator: Address, meta: Meta, royalties: [LicensedNFT.Royalty]) {
            self.id = id
            self.parentId = parentId
            self.creator = creator
            self.meta = meta
            self.royalties = royalties
            emit Minted(
                id: id,
                meta: meta,
                creator: creator,
                royalties: royalties,
            )
        }

        pub fun getMeta(): Meta {
            return self.meta
        }

        pub fun getRoyalties(): [LicensedNFT.Royalty] {
            return self.royalties
        }

        destroy() {
            emit Burned(id: self.id)
        }
    }

    pub resource Collection: NonFungibleToken.Provider, NonFungibleToken.Receiver, NonFungibleToken.CollectionPublic, LicensedNFT.CollectionPublic {
        pub var ownedNFTs: @{UInt64: NonFungibleToken.NFT}

        pub fun deposit(token: @NonFungibleToken.NFT) {
            let token <- token as! @RaribleNFTv2.NFT
            let id: UInt64 = token.id
            let dummy <- self.ownedNFTs[id] <- token
            destroy dummy
            emit Deposit(id: id, to: self.owner?.address)
        }

        pub fun withdraw(withdrawID: UInt64): @NonFungibleToken.NFT {
            let token <- self.ownedNFTs.remove(key: withdrawID) ?? panic("Missing NFT")
            emit Withdraw(id: token.id, from: self.owner?.address)
            return <- token
        }

        pub fun getIDs(): [UInt64] {
            return self.ownedNFTs.keys
        }

        pub fun borrowNFT(id: UInt64): &NonFungibleToken.NFT {
            return &self.ownedNFTs[id] as &NonFungibleToken.NFT
        }

        pub fun getRoyalties(id: UInt64): [LicensedNFT.Royalty] {
            let ref = &self.ownedNFTs[id] as auth &NonFungibleToken.NFT
            return (ref as! &LicensedNFT.NFT).getRoyalties()
        }

        init() {
            self.ownedNFTs <- {}
        }

        destroy () {
            destroy self.ownedNFTs
        }
    }

    access(account) fun mint(
        receiver: Capability<&{NonFungibleToken.CollectionPublic}>,
        parentId: UInt64,
        creator: Address,
        meta: Meta,
        royalties: [LicensedNFT.Royalty],
    ) {
        let token <- create NFT(
            id: RaribleNFTv2.totalSupply,
            parentId: parentId,
            creator: creator,
            meta: meta,
            royalties: royalties,
        )
        RaribleNFTv2.totalSupply = RaribleNFTv2.totalSupply + 1
        receiver.borrow()!.deposit(token: <- token)
    }

    pub fun createEmptyCollection(): @NonFungibleToken.Collection {
        return <- create Collection()
    }

    init() {
        self.totalSupply = 0

        self.CollectionPublicPath = /public/RaribleNFTv2
        self.CollectionStoragePath = /storage/RaribleNFTv2

        emit ContractInitialized()
    }
}
