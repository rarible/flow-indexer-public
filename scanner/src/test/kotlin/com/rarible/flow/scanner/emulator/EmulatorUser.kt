package com.rarible.flow.scanner.emulator

import com.nftco.flow.sdk.FlowAddress

enum class EmulatorUser(val address: FlowAddress, val keyHex: String, val pubHex: String) {

    Patrick(
        address = FlowAddress("0x01cf0e2f2f715450"),
        keyHex = "71f64f0287a49382b1476323f363563bc32d8cdd8d81ba335e0b76f14555e5d2",
        pubHex = "08bdf0842d8405f0215259694cbb6cd44f6a6320976921be12c2609d374d456835fa9c73aa6e6bdefffbea807451e3230417bc25dea96c04d53af86cbdd0bb9f"
    ),
    Squidward(
        address = FlowAddress("0x179b6b1cb6755e31"),
        keyHex = "5bd89c2dbacd509e79065e65be99655946cdb339e912efc1e55e02254939e704",
        pubHex = "dfd1bdb38fc8b7b397edc7058564edc07760a0760c7e8bfff819f8d9763f0e73c5755d8ab5c367714949c9e7ce0015948b931bdb7fc2591fd215bc75908d03cd"
    ),
    Gary(
        address = FlowAddress("0xf3fcd2c1a78f5eee"),
        keyHex = "6fcee83ae408d909aaf73a88c8a99ace0af5a77aa2cd3990b7ead55fc097e52c",
        pubHex = "74c3da5436374b0fd36bbe6e8d2c0a1e4437a029e6ac793d2c1d9b6f352fd1fa2e7fcb5b6b3851f21604fc092c2135916d2931cd2c641f063d603c687ec390dd"
    ),
    Emulator(
        address = FlowAddress("0xf8d6e0586b0a20c7"),
        keyHex = "8ea7b6cb8da7a09c19e2401eafcfd3863136decb5a495779a22f917c376da8b4",
        pubHex = "d11ab177438697df0aca15ae037722be4d815194722ccdb3ebebdfdbd7d41934ff646e10f6c7e01d13865f15f58cd7445b2ebc9709287cb490e46f0290a3c733"
    )
}
