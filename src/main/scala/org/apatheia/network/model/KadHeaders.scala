package org.apatheia.network.model

import org.apatheia.model.NodeId

final case class KadHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId
)
