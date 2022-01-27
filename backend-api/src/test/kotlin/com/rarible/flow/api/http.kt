package com.rarible.flow.api

import org.springframework.test.web.reactive.server.WebTestClient


object http {

    inline fun <reified T: Any> WebTestClient.shouldGetPaginatedResult(url: String, params: Map<String, Any> = emptyMap()): T {
        return this.get()
            .uri(url, params)
            .exchange()
            .expectStatus().isOk
            .expectBody(T::class.java)
            .returnResult().responseBody!!
    }

    inline fun <reified T: Any> WebTestClient.shouldGetPaginatedResult(url: String, vararg params: Pair<String, Any>): T {
        return shouldGetPaginatedResult(url, mapOf(*params))
    }

    fun WebTestClient.shouldGetBadRequest(url: String, params: Map<String, Any> = emptyMap()): WebTestClient.ResponseSpec {
        return this.get()
            .uri(url, params)
            .exchange()
            .expectStatus().isBadRequest
    }

    fun WebTestClient.shouldGetBadRequest(url: String, vararg params: Pair<String, Any>): WebTestClient.ResponseSpec  {
        return shouldGetBadRequest(url, mapOf(*params))
    }
}