package org.apatheia.network.model

import java.nio.charset.StandardCharsets

trait Tag {

  def tag: String
  def tagData: Array[Byte] = tag.getBytes(StandardCharsets.UTF_8)

}

object Tag {

  case object Contact extends Tag {
    override val tag: String = "[CONTACT]"
  }

}
