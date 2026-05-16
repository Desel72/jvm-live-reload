enablePlugins(LiveReloadPlugin)
enablePlugins(BuildInfoPlugin)

scalaVersion := "2.13.16"

resolvers += Resolver.mavenLocal
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-http" % "3.5.1",
  "dev.zio" %% "zio-config" % "4.0.5",
  "dev.zio" %% "zio-config-magnolia" % "4.0.5",
  "dev.zio" %% "zio-config-typesafe" % "4.0.5",
  "org.slf4j" % "slf4j-simple" % "2.0.16"
)

val isSbt2 = settingKey[Boolean]("isSbt2")
isSbt2 := (sbtBinaryVersion.value match {
  case "2" => true
  case _   => false
})

val proxyPort = settingKey[Int]("proxyPort")
proxyPort := sys.props.get("testkit.proxyPort").map(_.toInt).getOrElse(if (isSbt2.value) 9001 else 9000)

val port = settingKey[Int]("port")
port := sys.props.get("testkit.port").map(_.toInt).getOrElse(if (isSbt2.value) 8081 else 8080)

liveDevSettings := Seq(
  DevSettingsKeys.LiveReloadProxyHttpPort -> proxyPort.value.toString,
  DevSettingsKeys.LiveReloadHttpPort -> port.value.toString,
  DevSettingsKeys.LiveReloadIsDebug -> "true"
)

buildInfoKeys := Seq[BuildInfoKey](port)
buildInfoPackage := "me.seroperson"
