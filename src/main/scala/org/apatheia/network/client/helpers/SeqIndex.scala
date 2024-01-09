package org.apatheia.network.client.helpers

import org.apatheia.network.model.Tag

object SeqIndex {
  def findPatterns(
      patternTag: Tag,
      data: Array[Byte]
  ): Seq[Int] = {
    val patternLength = patternTag.tagData.length
    val dataLength = data.length

    if (patternLength > dataLength) {
      Seq.empty[Int]
    } else {
      val maxStartIndex = dataLength - patternLength
      val patternStartByte = patternTag.tagData(0)

      (0 to maxStartIndex)
        .filter { i =>
          data(i) == patternStartByte && patternTag.tagData.indices.forall(j =>
            data(i + j) == patternTag.tagData(j)
          )
        }
        .map(
          _ + patternTag.tagData.size
        )
    }
  }

}
