import NonFungibleToken from "flow/NonFungibleToken.cdc"
import MetadataViews from "flow/MetadataViews.cdc"
import LicensedNFT from "LicensedNFT.cdc"
import RaribleNFTv2 from "RaribleNFTv2.cdc"

pub contract SoftCollection : NonFungibleToken, LicensedNFT {

    pub var totalSupply: UInt64

    pub let CollectionPublicPath: PublicPath
    pub let CollectionStoragePath: StoragePath
    pub let MinterStoragePath: StoragePath

    pub event ContractInitialized()
    pub event Withdraw(id: UInt64, from: Address?)
    pub event Deposit(id: UInt64, to: Address?)
    pub event Minted(id: UInt64, parentId: UInt64?, meta: Meta, creator: Address, royalties: [LicensedNFT.Royalty])
    pub event Burned(id: UInt64)
    pub event Changed(id: UInt64, meta: Meta)

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
        pub let symbol: String
        pub var icon: String?
        pub var description: String?
        pub var url: String?

        init (name: String, symbol: String, icon: String?, description: String?, url: String?) {
            self.name = name
            self.symbol = symbol
            self.icon = icon
            self.description = description
            self.url = url
        }

        pub fun update(icon: String?, description: String?, url: String?) {
            self.icon = icon
            self.description = description
            self.url = url
        }
    }

    pub resource NFT: NonFungibleToken.INFT, MetadataViews.Resolver {
        pub let id: UInt64
        pub let parentId: UInt64?
        pub let creator: Address
        access(self) let meta: Meta
        access(self) let royalties: [LicensedNFT.Royalty]

        init (id: UInt64, parentId: UInt64?, meta: Meta, creator: Address, royalties: [LicensedNFT.Royalty]) {
            self.id = id
            self.parentId = parentId
            self.meta = meta
            self.creator = creator
            self.royalties = royalties
            emit Minted(
                id: id,
                parentId: parentId,
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

        pub fun getViews(): [Type] {
            return [Type<MetadataViews.Display>()]
        }

        pub fun update(icon: String?, description: String?, url: String?) {
            self.meta.update(icon: icon, description: description, url: url)
            emit Changed(id: self.id, meta: self.meta)
        }

        pub fun resolveView(_ view: Type): AnyStruct? {
            switch view {
                case Type<MetadataViews.Display>():
                    return MetadataViews.Display(
                        name: self.meta.name,
                        description: self.meta.description ?? "",
                        thumbnail: MetadataViews.HTTPFile(url: self.meta.url ?? ""),
                    )
            }
            return nil
        }

        destroy() {
            emit Burned(id: self.id)
        }
    }

    pub resource Collection: NonFungibleToken.Provider, NonFungibleToken.Receiver, NonFungibleToken.CollectionPublic, MetadataViews.ResolverCollection, LicensedNFT.CollectionPublic {
        pub var ownedNFTs: @{UInt64: NonFungibleToken.NFT}

        pub fun deposit(token: @NonFungibleToken.NFT) {
            let token <- token as! @SoftCollection.NFT
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

        pub fun borrowViewResolver(id: UInt64): &{MetadataViews.Resolver} {
            let authRef = &self.ownedNFTs[id] as auth &NonFungibleToken.NFT
            let ref = authRef as! &NFT
            return ref as! &{MetadataViews.Resolver}
        }

        pub fun getRoyalties(id: UInt64): [LicensedNFT.Royalty] {
            let ref = &self.ownedNFTs[id] as auth &NonFungibleToken.NFT
            return (ref as! &LicensedNFT.NFT).getRoyalties()
        }

        pub fun borrowSoftCollection(id: UInt64): &NFT? {
            let ref = &self.ownedNFTs[id] as auth &NonFungibleToken.NFT
            return ref as! &NFT
        }

        pub fun mint(
            softId: UInt64,
            receiver: Capability<&{NonFungibleToken.CollectionPublic}>,
            meta: RaribleNFTv2.Meta,
            royalties: [LicensedNFT.Royalty],
        ) {
            let ref = &self.ownedNFTs[softId] as auth &NonFungibleToken.NFT
            let minter = ref as! &NFT

            RaribleNFTv2.mint(
                receiver: receiver,
                parentId: minter.id,
                creator: minter.creator,
                meta: meta,
                royalties: royalties,
            )
        }

        pub fun updateItem(id: UInt64, icon: String?, description: String?, url: String?) {
            let ref = &self.ownedNFTs[id] as auth &NonFungibleToken.NFT
            let minter = ref as! &NFT
            minter.update(icon: icon, description: description, url: url)
        }

        init() {
            self.ownedNFTs <- {}
        }

        destroy () {
            destroy self.ownedNFTs
        }
    }

    pub resource Minter {
        pub fun mint(
            receiver: Capability<&{NonFungibleToken.CollectionPublic}>,
            parentId: UInt64?,
            name: String,
            symbol: String,
            icon: String?,
            description: String?,
            url: String?,
            supply: UInt64?,
            royalties: [LicensedNFT.Royalty],
        ) {
            let meta = Meta(
                name: name,
                symbol: symbol,
                icon: icon,
                description: description,
                url: url,
            )
            let token <- create NFT(
                id: SoftCollection.totalSupply,
                parentId: parentId,
                meta: meta,
                creator: receiver.address,
                royalties: royalties,
            )
            SoftCollection.totalSupply = SoftCollection.totalSupply + 1
            receiver.borrow()!.deposit(token: <- token)
        }
    }

    pub fun createEmptyCollection(): @NonFungibleToken.Collection {
        return <- create Collection()
    }

    init() {
        self.totalSupply = 0

        self.CollectionPublicPath = /public/SoftCollection
        self.CollectionStoragePath = /storage/SoftCollection
        self.MinterStoragePath = /storage/SoftCollectionMinter

        self.account.save(<- create Minter(), to: self.MinterStoragePath)

        emit ContractInitialized()
    }
}
