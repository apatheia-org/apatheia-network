package org.apatheia.network.model

final case class KadDatagramPayload(
    command: KadCommand,
    parameters: List[KadParameter]
)
