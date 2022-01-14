# Rarible Protocol Flow Indexer

Spring Boot Application for indexing [Flow](https://onflow.org/) blockchain, and API for reading NFT.

For more information, see [Rarible Protocol Flow documentation](https://docs.rarible.org/flow/flow-overview/).

### Services

| Service | Description |
|---|---|
| backend-api  | [Flow API](https://github.com/rarible/flow-protocol-api) implementation |
| scanner | Core indexing functionality |

For reading NFT events, we need to add `Subscriber` (see `com.rarible.flow.scanner.subscriber` package) with the event's description (usually contract name and event name) and start block height.

The service uses Kafka to exchange messages with other Rarible services.

The service also uses MongoDB as a persistence storage.

### Important properties

| Property | Description |
|---|---|
| blockchain.scanner.flow.chainId  | Flow network for indexing (MAINNET, TESTNET, EMULATOR) |

Use `gradle build` task to build artifact's.

### Suggestions

You are welcome to [suggest features](https://github.com/rarible/protocol/discussions) and [report bugs found](https://github.com/rarible/protocol/issues)!

### License

[GPL v3 license](LICENSE) is used for all services and other parts of the indexer.
