/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck.util

import scala.collection.mutable
import scala.collection.{Map as _, *}

trait Buildable[T, C] extends Serializable {
  def builder: mutable.Builder[T, C]
  def fromIterable(it: Traversable[T]): C = {
    val b = builder
    b ++= it
    b.result()
  }
}

object Buildable extends BuildableVersionSpecific {
  import java.util.{ArrayList, HashMap}

  implicit def buildableArrayList[T]: Buildable[T, ArrayList[T]] =
    new Buildable[T, ArrayList[T]] {
      def builder: mutable.Builder[T, ArrayList[T]] = new ArrayListBuilder[T]
    }

  implicit def buildableHashMap[K, V]: Buildable[(K, V), HashMap[K, V]] =
    new Buildable[(K, V), HashMap[K, V]] {
      def builder = new HashMapBuilder[K, V]
    }

  def buildableSeq[T]: Buildable[T, Seq[T]] =
    new Buildable[T, Seq[T]] {
      def builder: mutable.Builder[T, Seq[T]] =
        Seq.newBuilder[T]
    }

  private[scalacheck] implicit def implicitBuildableSeq[T]: Buildable[T, Seq[T]] = buildableSeq
}
