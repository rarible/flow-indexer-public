package com.rarible.flow.scanner.flowty

import io.netty.util.internal.ThreadLocalRandom

class SimpleUserAgentProvider {
    fun get(): String {
        val template = agentsTemplate.random()
        return template
            .replace(version1, randomVersion())
            .replace(version2, randomVersion())
            .replace(version3, randomVersion())
    }

    private fun randomVersion(): String {
        val current = ThreadLocalRandom.current()
        return "${current.nextInt(1, 30)}.${current.nextInt(0, 100)}.${current.nextInt(0, 200)}"
    }

    private companion object {
        const val version1 = "#version1"
        const val version2 = "#version2"
        const val version3 = "#version3"

        val agentsTemplate = listOf(
            "Mozilla/$version1 (Macintosh; U; PPC Mac OS X; fr-fr) AppleWebKit/$version2 (KHTML, like Gecko) Safari/$version3",
            "Opera/$version1 (X11; Linux x86_64; U; de) Presto/$version2 Version/$version3",
            "Mozilla/$version1 (Windows NT 10.0; Win64; x64) AppleWebKit/$version2 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/$version3",
            "Mozilla/$version1 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$version2.63 Safari/$version3"
        )
    }
}
