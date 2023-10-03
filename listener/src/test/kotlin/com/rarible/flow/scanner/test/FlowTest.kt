package com.rarible.flow.scanner.test

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(FlowTestExtension::class)
annotation class FlowTest

class FlowTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        FlowTestContainer.ping()
    }
}
