package org.apatheia.network.model

import org.apatheia.model.Contact

final case class KadResponsePayload(contact: Contact, data: Array[Byte])
