import zio._
import zio.http._
import zio.http.netty.NettyConfig

object GreetingServer extends ZIOAppDefault {
  val routes =
    Routes(
      Method.GET / "greet" -> handler { (req: Request) =>
        Response.text(Text.response)
      },
      Method.GET / "health" -> handler { (req: Request) =>
        Response.ok
      }
    )
  def run = Server
    .serve(routes)
    .provide(
      Server.customized,
      ZLayer.succeed(Server.Config.default.port(me.seroperson.BuildInfo.port)),
      ZLayer.succeed(NettyConfig.defaultWithFastShutdown)
    )
}
