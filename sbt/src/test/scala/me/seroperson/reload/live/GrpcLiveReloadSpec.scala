package me.seroperson.reload.live

import io.grpc.{CallOptions, ManagedChannelBuilder, MethodDescriptor}
import io.grpc.MethodDescriptor.{Marshaller, MethodType}
import io.grpc.stub.ClientCalls
import java.io.{ByteArrayInputStream, InputStream}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class GrpcLiveReloadSpec extends LiveReloadBase {

  private def hexToBytes(hex: String): Array[Byte] =
    hex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  private def bytesToHex(bytes: Array[Byte]): String =
    bytes.map(b => f"${b & 0xff}%02x").mkString

  private val byteMarshaller: Marshaller[Array[Byte]] =
    new Marshaller[Array[Byte]] {
      override def stream(value: Array[Byte]): InputStream =
        new ByteArrayInputStream(value)
      override def parse(stream: InputStream): Array[Byte] =
        stream.readAllBytes()
    }

  private def verifyGrpc(
      service: String,
      method: String,
      requestHex: String,
      expectedResponseHex: String,
      port: Int
  ): Unit = {
    val methodDescriptor = MethodDescriptor
      .newBuilder(byteMarshaller, byteMarshaller)
      .setType(MethodType.UNARY)
      .setFullMethodName(s"$service/$method")
      .build()

    @tailrec
    def attempt(remaining: Int): Unit = {
      val result = Try {
        val channel = ManagedChannelBuilder
          .forAddress("localhost", port)
          .usePlaintext()
          .build()
        try {
          val response = ClientCalls.blockingUnaryCall(
            channel.newCall(methodDescriptor, CallOptions.DEFAULT),
            hexToBytes(requestHex)
          )
          val responseHex = bytesToHex(response)
          assert(
            responseHex == expectedResponseHex,
            s"GRPC $service/$method: expected $expectedResponseHex, got $responseHex"
          )
        } finally {
          channel.shutdownNow()
        }
      }
      result match {
        case Success(_) => ()
        case Failure(_) if remaining > 0 =>
          Thread.sleep(RetryInterval)
          attempt(remaining - 1)
        case Failure(ex) =>
          throw new AssertionError(
            s"Failed to verify GRPC $service/$method after $MaxRetries attempts: ${ex.getMessage}",
            ex
          )
      }
    }

    attempt(MaxRetries)
  }

  // sbt-protoc is only available for sbt 1.x
  test("grpc-scalapb - live reload on source change") {
    withSbt1Runner("grpc-scalapb") { (runner, proxyPort) =>
      runner.run("bgRun")
      // request: HelloRequest(name = "Hello World!")
      // response: HelloReply(message = "World Hello!")
      verifyGrpc(
        "Greeter",
        "SayHello",
        "0a0c48656c6c6f20576f726c6421",
        "0a0c576f726c642048656c6c6f21",
        proxyPort
      )
      runner.copyFile("changes/App.scala.1", "src/main/scala/App.scala")
      // response: HelloReply(message = "Hello World Reloaded!")
      verifyGrpc(
        "Greeter",
        "SayHello",
        "0a0c48656c6c6f20576f726c6421",
        "0a1548656c6c6f20576f726c642052656c6f6164656421",
        proxyPort
      )
    }
  }
}
