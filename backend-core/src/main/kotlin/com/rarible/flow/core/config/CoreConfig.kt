package com.rarible.flow.core.config

import com.rarible.flow.core.repository.ItemMetaRepository
import com.rarible.flow.core.repository.ItemRepository
import com.rarible.flow.core.repository.OrderRepository
import com.rarible.flow.core.repository.OwnershipRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableMongoAuditing
@EnableReactiveMongoRepositories(basePackages = [
    "com.rarible.flow.core.repository"
])
data class CoreConfig(
    val mongoTemplate: ReactiveMongoTemplate
) {
    @Bean
    fun itemRepository() = ItemRepository(mongoTemplate)

    @Bean
    fun ownershipRepo() = OwnershipRepository(mongoTemplate)

    @Bean
    fun orderRepository() = OrderRepository(mongoTemplate)

    @Bean
    fun itemMetaRepository() = ItemMetaRepository(mongoTemplate)
}
