package org.apatheia.algorithm.findnode

import cats.effect.kernel.Async
import org.apatheia.model.Contact

final case class DefaultFindNodeClient[F[_]: Async]()
    extends FindNodeClient[F] {
  override def requestContacts(nodeContact: Contact): F[List[Contact]] = ???
}
