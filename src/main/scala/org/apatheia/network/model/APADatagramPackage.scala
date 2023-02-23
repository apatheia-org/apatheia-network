package org.apatheia.network.model

case class APADatagramPackage(
    headers: APAHeaders,
    payload: APADatagramPayload
)
