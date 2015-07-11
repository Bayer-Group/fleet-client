package com.monsanto.arch.fleet

import spray.http.HttpHeaders.RawHeader

/**
 * Holds fleet configuration
 * @param urlBase base URL
 * @param headers headers, for e.g. authentication
 */
case class FleetConfig(urlBase: String, headers: List[RawHeader] = List.empty) {
  object URLs {
    private val base = s"$urlBase/fleet/v1"

    val units     = s"$base/units"
    val state     = s"$base/state"
    val machines  = s"$base/machines"
    def unit(name: String)  = units + s"/$name"
  }
}
