package com.rarible.flow.scanner.subscriber.meta

import com.rarible.flow.Contracts
import org.springframework.stereotype.Component

//@Component
class HWOtherGarageCardV2MetaSubscriber: HWGarageCardV2MetaSubscriber() {
    override val name = "hw_other_meta_card_v2"
    override val contract = Contracts.HW_OTHER_GARAGE_PM_V2
}