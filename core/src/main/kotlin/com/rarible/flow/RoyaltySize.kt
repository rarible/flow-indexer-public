package com.rarible.flow

object RoyaltySize {
    const val TEN_PERCENT = 0.1
    const val FIVE_PERCENT = 0.05

    fun Double.percent() = this.div(100)
}