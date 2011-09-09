package me.winslow.d.mn2gt

import driver._, xml._, Mapnik2GeoTools._

object Driver {
  // quick n' dirty command line parser
  // supports --switch=foo and --switch foo syntax.
  // -- stops parsing, everything else is a generic argument
  def parseOpts(args: Seq[String]): (Map[String, String], Seq[String]) =
    args match {
      case Seq() => (Map.empty, Seq.empty)
      case Seq("--", xs @ _*) => (Map.empty, xs)
      case Seq(switch, xs @ _*) 
        if (switch startsWith "--") && (switch contains "=")
        =>
        val name = switch drop 2 takeWhile("=" !=)
        val value = switch dropWhile("=" !=) tail
        val (switches, args) = parseOpts(xs)
        (switches + (name -> value), args)
      case Seq(switch, value, xs @ _*) if switch startsWith "--" =>
        val name = switch drop 2
        val (switches, args) = parseOpts(xs)
        (switches + (name -> value), args)
      case Seq(x) => (Map.empty, Seq(x))
      case Seq(x, xs @ _*) => 
        val (switches, args) = parseOpts(xs)
        (switches, x +: args)
    }

  def getOperation(
    source: java.io.File,
    switches: Map[String, String]
  ): Operation = {
    if (Set("rest", "datadir") subsetOf switches.keySet)
      PublishToGeoServer(
        source,
        GeoServerConnection(
          switches("rest"),
          switches.getOrElse("user", "admin"),
          switches.getOrElse("password", "geoserver"),
          switches("datadir"),
          switches.getOrElse("prefix", "mn2gt"),
          switches.getOrElse("namespace", "http://mn2gt.com/")
        )
      )
    else {
      val out = switches.get("output").map(new java.io.File(_))
      val resolved = 
        if (out forall (_ isAbsolute))
          out.get
        else
          new java.io.File(source.getParent(), switches.getOrElse("output", "output"))
      LocalConversion(source, resolved) 
    }
  }

  def main(args: Array[String]) {
    val (switches, files) = parseOpts(args)
    files.foreach { f => 
      getOperation(new java.io.File(f), switches).run()
    }
  }
}
