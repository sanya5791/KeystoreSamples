package com.akhutornoy.tastekeystore.data

interface Repository {
    var signature: String
    var encryptedRsaString: String
}