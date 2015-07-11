package com.monsanto.arch.fleet

import spray.http.HttpResponse

/**
 * Lightweight wrapper for operation failures
 * @param status HTTP status code
 * @param body body of failure notification as string
 */
case class OperationFailedException(status:Int, body:String) extends
  RuntimeException(s"Status: $status, Body: $body")

object OperationFailedException{
  def apply(r: HttpResponse) = new OperationFailedException(r.status.intValue,if (r.entity.data.length <1024) r.entity.asString else r.entity.data.length + " bytes")
}
