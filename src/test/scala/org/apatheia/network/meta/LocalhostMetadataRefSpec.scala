package org.apatheia.network.meta

import org.apatheia.model.Contact

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apatheia.network.model.LocalhostMetadata
import org.apatheia.model.NodeId
import cats.effect.std.AtomicCell
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.apatheia.model.RoutingTable

class LocalhostMetadataRefSpec extends AnyFlatSpec with Matchers {

  "get()" should "return network metadata from the localhost" in new TestContext {
    val resultEffect = for {
      cell <- AtomicCell[IO].of[LocalhostMetadata](
        localhostMetadata
      )
      defaultLocalhostMetadataRef = DefaultLocalhostMetadataRef[IO](cell)
      response <- defaultLocalhostMetadataRef.get
    } yield (response)

    val result = resultEffect.unsafeRunSync()

    result shouldBe localhostMetadata
  }

  "modify()" should "update localhost network metadata" in new TestContext {
    val resultEffect = for {
      cell <- AtomicCell[IO].of[LocalhostMetadata](
        localhostMetadata
      )
      defaultLocalhostMetadataRef = DefaultLocalhostMetadataRef[IO](cell)
      responseBeforeModify <- defaultLocalhostMetadataRef.get
      _ <- defaultLocalhostMetadataRef.modify(updatedLocalhostMetadata)
      responseAfterModify <- defaultLocalhostMetadataRef.get
    } yield (responseBeforeModify, responseAfterModify)

    val result = resultEffect.unsafeRunSync()

    result._1 shouldBe localhostMetadata
    result._2 shouldBe updatedLocalhostMetadata
  }

  trait TestContext {
    val localhostMetadata = LocalhostMetadata(
      localContact = Contact(
        nodeId = NodeId(1),
        port = 1000,
        ip = "0.0.0.0"
      ),
      routingTable = RoutingTable(NodeId(1), List.empty, 20)
    )

    val updatedLocalhostMetadata = localhostMetadata.copy(
      localContact = localhostMetadata.localContact.copy(nodeId = NodeId(2))
    )
  }

}
