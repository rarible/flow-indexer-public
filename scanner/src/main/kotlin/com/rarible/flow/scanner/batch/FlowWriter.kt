package com.rarible.flow.scanner.batch

import com.rarible.flow.scanner.model.FlowBlock
import com.rarible.flow.scanner.model.FlowTransaction
import com.rarible.flow.scanner.repo.FlowBlockRepository
import com.rarible.flow.scanner.repo.FlowTransactionRepository
import org.springframework.batch.item.ItemWriter

/**
 * Created by TimochkinEA at 10.06.2021
 */
class FlowWriter(
    private val blockRepository: FlowBlockRepository,
    private val txRepository: FlowTransactionRepository
): ItemWriter<Pair<FlowBlock, List<FlowTransaction>>> {
    override fun write(items: MutableList<out Pair<FlowBlock, List<FlowTransaction>>>) {
        items.forEach { p ->
            blockRepository.save(p.first).subscribe()
            if (p.second.isNotEmpty()) {
                txRepository.saveAll(p.second).subscribe()
            }
        }
    }
}
