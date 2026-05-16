import sbt._

object Dependencies {

  val undertow = "io.undertow" % "undertow-core" % "2.3.20.Final"

  // GRPC dependencies
  val grpcVersion = "1.72.0"
  val grpcNettyShaded = "io.grpc" % "grpc-netty-shaded" % grpcVersion
  val grpcStub = "io.grpc" % "grpc-stub" % grpcVersion
  val grpcServices = "io.grpc" % "grpc-services" % grpcVersion

  val playFileWatch = "org.playframework" % "play-file-watch" % "3.0.0-M4"
  val jline = "org.jline" % "jline" % "3.30.6"

  val zio = "dev.zio" %% "zio" % "2.1.24"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.6.3"

}
