package com.rarible.protocol.order.listener.metric

import com.rarible.core.telemetry.metrics.CountingMetric

class OrderMetric(root: String, type: String) : CountingMetric(
    "$root.order.$type", tag("blockchain", "flow")
)
