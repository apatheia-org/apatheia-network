package org.apatheia.network.model

import org.apatheia.model.Contact

final case class APAHeaders(
    from: Contact,
    to: Contact,
    opId: OpId
)
