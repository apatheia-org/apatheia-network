package org.apatheia.network.model

final case class APADatagramPayload(
    command: APACommand,
    parameters: List[APAParameter]
)
