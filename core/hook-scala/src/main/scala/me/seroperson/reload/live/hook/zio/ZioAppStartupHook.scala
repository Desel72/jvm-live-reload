package me.seroperson.reload.live.hook.zio;

import me.seroperson.reload.live.build.BuildLogger
import me.seroperson.reload.live.hook.Hook
import me.seroperson.reload.live.hook.zio.ZioAppStartupHook.PermanentThreadNames
import me.seroperson.reload.live.reflect.{MiscUtils, ShutdownHook}
import me.seroperson.reload.live.settings.DevServerSettings

class ZioAppStartupHook extends Hook {

  override def description: String = "Starts a zio.ZIOApp"

  override def isAvailable: Boolean =
    MiscUtils.hasClass("zio.ZIOApp$")

  override def hook(
      th: Thread,
      cl: ClassLoader,
      settings: DevServerSettings,
      logger: BuildLogger
  ) = {
    MiscUtils.dumpThreads(logger, th.getThreadGroup)

    // We need to update Context ClassLoader on all ZScheduler workers
    // because they usually survive reload
    MiscUtils.updateContextClassLoader(
      th.getThreadGroup,
      v => Option(v).exists(nn => PermanentThreadNames.exists(nn.getName.startsWith)),
      cl
    )
    logger.debug(
      s"Set $cl as a context classloader zio computing threads"
    )
  }

}

object ZioAppStartupHook {
  val PermanentThreadNames = Set("ZScheduler", "zio", "globalEventExecutor")
}