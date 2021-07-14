package com.rarible.flow.api.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
//
//@SpringBootTest(
//    properties = [
//        "application.environment = dev",
//        "spring.cloud.service-registry.auto-registration.enabled = false",
//        "spring.cloud.discovery.enabled = false",
//        "spring.cloud.consul.config.enabled = false",
//        "logging.logstash.tcp-socket.enabled = false",
//    ],
//    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
//)
//@ActiveProfiles("test")
internal class EchoWSControllerTest {

    @LocalServerPort
    private var localPort: Int = 0

    private val msg = "some small msg"

    //@Test
    fun test() {
        val queue1 = ArrayBlockingQueue<String>(1)
        val queue2 = ArrayBlockingQueue<String>(1)
        val stompClient = WebSocketStompClient(
            SockJsClient(
                listOf(
                    WebSocketTransport(
                        StandardWebSocketClient()
                    )
                )
            )
        )

        stompClient.messageConverter = StringMessageConverter()

        val session = stompClient.connect("ws://localhost:$localPort/ws", object : StompSessionHandlerAdapter() {})
            .get(1, TimeUnit.SECONDS)

        session.subscribe("/api/ping", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type = String::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                queue2.add(payload as String)
            }

        })

        session.subscribe("/topic/echo", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type = String::class.java

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                queue1.add(payload as String)
            }
        })

        session.send("/api/echo", msg)
        assertEquals(msg.uppercase(), queue1.poll(1, TimeUnit.SECONDS))
        assertEquals("pong", queue2.poll(1, TimeUnit.SECONDS))
    }
}
