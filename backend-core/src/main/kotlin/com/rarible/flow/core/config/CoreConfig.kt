package com.rarible.flow.core.config

import com.rarible.flow.core.converter.FlowConversions
import com.rarible.flow.core.converter.ItemIdConversions
import com.rarible.flow.core.converter.OwnershipIdConversions
import com.rarible.flow.core.repository.OrderRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableMongoAuditing
@EnableReactiveMongoRepositories(basePackages = [
    "com.rarible.flow.core.repository"
])
class CoreConfig {

    @Bean
    fun orderRepository(mongoTemplate: ReactiveMongoTemplate) = OrderRepository(mongoTemplate)

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            FlowConversions + ItemIdConversions + OwnershipIdConversions
        )
    }
}
