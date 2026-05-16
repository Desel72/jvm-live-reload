val ScalaPBVersion = "0.11.17"
val GrpcVersion = "1.72.0"

enablePlugins(LiveReloadPlugin)
enablePlugins(BuildInfoPlugin)

scalaVersion := "2.13.16"
resolvers += Resolver.mavenLocal

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % GrpcVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % ScalaPBVersion
)

val isSbt2 = settingKey[Boolean]("isSbt2")
isSbt2 := (sbtBinaryVersion.value match {
  case "2" => true
  case _   => false
})

val proxyPort = settingKey[Int]("proxyPort")
proxyPort := (if (isSbt2.value) 9001 else 9000)

val port = settingKey[Int]("port")
port := (if (isSbt2.value) 8081 else 8080)

liveServerType := me.seroperson.reload.live.sbt.GrpcServerType

liveDevSettings := Seq(
  DevSettingsKeys.LiveReloadProxyGrpcPort -> proxyPort.value.toString,
  DevSettingsKeys.LiveReloadGrpcPort -> port.value.toString,
  DevSettingsKeys.LiveReloadIsDebug -> "true"
)

buildInfoKeys := Seq[BuildInfoKey](port)
buildInfoPackage := "me.seroperson"

InputKey[Unit]("verifyGrpcCall") := {
  import com.eed3si9n.expecty.Expecty.assert
  import _root_.io.grpc.CallOptions
  import _root_.io.grpc.ManagedChannel
  import _root_.io.grpc.ManagedChannelBuilder
  import _root_.io.grpc.MethodDescriptor
  import _root_.io.grpc.stub.ClientCalls
  import java.io.ByteArrayInputStream
  import java.io.InputStream
  import java.util.concurrent.TimeUnit

  class ByteArrayMarshaller
      extends MethodDescriptor.Marshaller[Array[Byte]] {
    override def stream(value: Array[Byte]): InputStream = new ByteArrayInputStream(
      value
    )

    override def parse(stream: InputStream): Array[Byte] = stream.readAllBytes()
  }

  def hexStringToBytes(hex: String): Array[Byte] = {
    new java.math.BigInteger(hex, 16).toByteArray
  }

  val args = Def.spaceDelimited("<expected_response>").parsed
  val serviceName = args(0)
  val methodName = args(1)
  val request = hexStringToBytes(args(2))
  val expectedResponse = hexStringToBytes(args(3)).toList

    val channel: ManagedChannel =
      ManagedChannelBuilder.forAddress("localhost", proxyPort.value).usePlaintext().build()
    try {
      val methodDescriptor =
        MethodDescriptor
          .newBuilder[Array[Byte], Array[Byte]]()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(s"$serviceName/$methodName")
          .setRequestMarshaller(new ByteArrayMarshaller())
          .setResponseMarshaller(new ByteArrayMarshaller())
          .build()

      val result = ClientCalls.blockingUnaryCall(
        channel,
        methodDescriptor,
        CallOptions.DEFAULT,
        request
      ).toList

      assert(result == expectedResponse)
    } finally {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

}
