package me.seroperson.reload.live.mill.test

import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import io.grpc.stub.ClientCalls
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import scala.util.Using

trait RequestMaker {

  private lazy val client = OkHttpClient()

  def runUntil(
      url: String,
      expectedStatus: Int,
      expectedBody: String
  ): Boolean = {
    val request: Request = Request.Builder().url(url).build()

    try {
      val (code, body) = Using(client.newCall(request).execute()) { response =>
        Using(response.body()) { body =>
          response.code -> body.string()
        }.get
      }.get
      println(s"Requesting $url, got $code and $body")
      if (expectedStatus == code && expectedBody == body) {
        return true
      } else {
        Thread.sleep(500)
        return runUntil(url, expectedStatus, expectedBody)
      }
    } catch {
      case ex: Exception =>
        println(s"Got exception: ${ex.getMessage}")
        Thread.sleep(500)
        return runUntil(url, expectedStatus, expectedBody)
    }
  }

  /** Runs GRPC call until expected response is received. Use
    * www.protobufpal.com to get request/response presentation in bytes.
    */
  def runGrpcUntil(
      host: String,
      port: Int,
      serviceName: String,
      methodName: String,
      request: Array[Byte],
      expectedResponse: Array[Byte]
  ): Boolean = {
    try {
      val response =
        grpcCall(host, port, serviceName, methodName, request)
      println(
        s"GRPC call to $serviceName/$methodName, got ${response.map("%02x".format(_)).mkString}"
      )
      if (response.sameElements(expectedResponse)) {
        return true
      } else {
        Thread.sleep(500)
        return runGrpcUntil(
          host,
          port,
          serviceName,
          methodName,
          request,
          expectedResponse
        )
      }
    } catch {
      case ex: Exception =>
        println(s"GRPC exception: ${ex.getMessage}")
        Thread.sleep(500)
        return runGrpcUntil(
          host,
          port,
          serviceName,
          methodName,
          request,
          expectedResponse
        )
    }
  }

  private def grpcCall(
      host: String,
      port: Int,
      serviceName: String,
      methodName: String,
      request: Array[Byte]
  ): Array[Byte] = {
    val channel: ManagedChannel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    try {
      val methodDescriptor =
        MethodDescriptor
          .newBuilder[Array[Byte], Array[Byte]]()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(s"$serviceName/$methodName")
          .setRequestMarshaller(ByteArrayMarshaller())
          .setResponseMarshaller(ByteArrayMarshaller())
          .build()

      return ClientCalls.blockingUnaryCall(
        channel,
        methodDescriptor,
        CallOptions.DEFAULT,
        request
      )
    } finally {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
  }

  private class ByteArrayMarshaller
      extends MethodDescriptor.Marshaller[Array[Byte]] {
    override def stream(value: Array[Byte]): InputStream = ByteArrayInputStream(
      value
    )

    override def parse(stream: InputStream): Array[Byte] = stream.readAllBytes()
  }
}

