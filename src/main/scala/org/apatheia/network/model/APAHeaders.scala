package org.apatheia.network.model

import org.apatheia.model.NodeId

final case class APAHeaders(
    from: NodeId,
    to: NodeId,
    opId: OpId
)
