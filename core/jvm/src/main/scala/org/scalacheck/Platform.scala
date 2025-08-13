/*
 * ScalaCheck
 * Copyright (c) 2007-2021 Rickard Nilsson. All rights reserved.
 * http://www.scalacheck.org
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package org.scalacheck

import scala.annotation.nowarn

import Test.*

private[scalacheck] object Platform {

  import util.FreqMap

  def runWorkers(
      params: Parameters,
      workerFun: Int => Result,
      stop: () => Unit
  ): Result = {
    import params.*

    def mergeResults(r1: Result, r2: Result): Result = {
      val Result(st1, s1, d1, fm1, _) = r1
      val Result(st2, s2, d2, fm2, _) = r2
      if (st1 != Passed && st1 != Exhausted)
        Result(st1, s1 + s2, d1 + d2, fm1 ++ fm2)
      else if (st2 != Passed && st2 != Exhausted)
        Result(st2, s1 + s2, d1 + d2, fm1 ++ fm2)
      else {
        if (s1 + s2 >= minSuccessfulTests && maxDiscardRatio * (s1 + s2) >= (d1 + d2))
          Result(Passed, s1 + s2, d1 + d2, fm1 ++ fm2)
        else
          Result(Exhausted, s1 + s2, d1 + d2, fm1 ++ fm2)
      }
    }

    if (workers < 2) workerFun(0)
    else {
      import concurrent.*
      val tp = java.util.concurrent.Executors.newFixedThreadPool(workers)
      implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(tp)
      try {
        val fs = List.range(0, workers) map (idx =>
          Future {
            params.customClassLoader.foreach(
              Thread.currentThread.setContextClassLoader(_)
            )
            blocking { workerFun(idx) }
          })
        val zeroRes = Result(Passed, 0, 0, FreqMap.empty[Set[Any]])
        val res =
          if (fs.isEmpty) Future.successful(zeroRes)
          else Future.sequence(fs).map(_.foldLeft(zeroRes)(mergeResults))
        Await.result(res, concurrent.duration.Duration.Inf)
      } finally {
        stop()
        tp.shutdown()
      }
    }
  }

  @nowarn("msg=is never used")
  def newInstance(name: String, loader: ClassLoader, paramTypes: Seq[Class[?]])(args: Seq[AnyRef]): AnyRef =
    if (args.nonEmpty) ???
    else Class.forName(name, true, loader).getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]

  def loadModule(name: String, loader: ClassLoader): AnyRef =
    Class.forName(name + "$", true, loader).getField("MODULE$").get(null)

  class EnableReflectiveInstantiation extends scala.annotation.Annotation

  def getClassLoader: ClassLoader = getClass.getClassLoader
}
