import Crypto

pub fun main(rawPublicKeys: [String], algorithm: SignatureAlgorithm, weights: [UFix64], signatures: [String], signedData: String): Bool {
  let keyList = Crypto.KeyList()
  var i = 0
  for rawPublicKey in rawPublicKeys {
    keyList.add(
      PublicKey(
        publicKey: rawPublicKey.decodeHex(),
        signatureAlgorithm: algorithm
      ),
      hashAlgorithm: HashAlgorithm.SHA3_256,
      weight: weights[i],
    )
    i = i + 1
  }

  let signatureSet: [Crypto.KeyListSignature] = []
  var j = 0
  for signature in signatures {
    signatureSet.append(
      Crypto.KeyListSignature(
        keyIndex: j,
        signature: signature.decodeHex()
      )
    )
    j = j + 1
  }

  return keyList.verify(
    signatureSet: signatureSet,
    signedData: signedData.utf8,
  )
}