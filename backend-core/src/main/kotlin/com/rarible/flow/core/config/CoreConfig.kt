package com.rarible.flow.core.config

import com.rarible.flow.core.converter.FlowConversions
import com.rarible.flow.core.converter.ItemIdConversions
import com.rarible.flow.core.converter.OwnershipIdConversions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Configuration
@EnableReactiveMongoAuditing(dateTimeProviderRef = "dateTimeProvider")
@EnableReactiveMongoRepositories(basePackages = [
    "com.rarible.flow.core.repository"
])
class CoreConfig {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            FlowConversions + ItemIdConversions + OwnershipIdConversions
        )
    }

    @Bean
    fun dateTimeProvider(): DateTimeProvider {
        return DateTimeProvider {
            Optional.of(LocalDateTime.now(ZoneOffset.UTC))
        }
    }
}
