# flow-nft-indexer

Spring Boot Application for indexing [Flow](https://onflow.org) blockchain, and API for read NFT.

## Services

| Service | Description |
|---|---|
| backend-api  | [Flow API](https://github.com/rarible/flow-protocol-api) implementation |
| scanner | Core indexing functionality |

For read NFT events we need to add `Subscriber` (see `com.rarible.flow.scanner.subscriber` package) with event's description (usually contract name and event name) and start block height

The service uses Kafka to exchange messages with other Rarible services. 

The service also uses MongoDB as persistence storage.

## Important properties

| Property | Description |
|---|---|
| blockchain.scanner.flow.chainId  | Flow network for indexing (MAINNET, TESTNET, EMULATOR) |


use 
`gradle build` task to build artifact's
