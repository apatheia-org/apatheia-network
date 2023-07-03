package org.apatheia.network.model

import org.apatheia.model.PackageDataParser
import org.apatheia.model.NodeId

trait BytesizedPackageData {
  val byteSize: Int
}

object BytesizedPackageData {
  implicit class NodeIdWithByteSize(x: PackageDataParser[NodeId])
      extends BytesizedPackageData {
    val byteSize: Int = 20
  }
}
