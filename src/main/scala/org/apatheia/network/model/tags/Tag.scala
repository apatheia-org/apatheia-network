package org.apatheia.network.model.tags

import java.nio.charset.StandardCharsets

trait Tag {

  def tag: String
  def tagData: Array[Byte] = tag.getBytes(StandardCharsets.UTF_8)

}
