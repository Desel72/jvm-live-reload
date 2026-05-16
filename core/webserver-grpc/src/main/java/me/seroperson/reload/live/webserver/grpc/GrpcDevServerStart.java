package me.seroperson.reload.live.webserver.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.List;
import me.seroperson.reload.live.BaseDevServerStart;
import me.seroperson.reload.live.build.BuildLink;
import me.seroperson.reload.live.build.BuildLogger;
import me.seroperson.reload.live.settings.DevServerSettings;

/**
 * Main entry point for the GRPC proxy development server.
 *
 * <p>This class manages the lifecycle of a GRPC proxy server that sits between GRPC clients and the
 * actual application server. It handles automatic reloading of the application when code changes
 * are detected.
 */
public class GrpcDevServerStart extends BaseDevServerStart<Server> {

  private ReloadableGrpcProxyHandler proxyHandler;

  /**
   * Creates a new GRPC development server.
   *
   * @param settings the development server settings
   * @param buildLink the build link for triggering recompilation
   * @param logger the logger for outputting messages
   * @param mainClass the main class to run
   * @param startupHookClasses list of startup hook class names
   * @param shutdownHookClasses list of shutdown hook class names
   */
  public GrpcDevServerStart(
      DevServerSettings settings,
      BuildLink buildLink,
      BuildLogger logger,
      String mainClass,
      List<String> startupHookClasses,
      List<String> shutdownHookClasses) {
    super(settings, buildLink, logger, mainClass, startupHookClasses, shutdownHookClasses);
  }

  @Override
  public void start() {
    if (settings.isDebug()) {
      dumpHooks();
    }

    String targetHost = settings.getGrpcHost();
    int targetPort = settings.getGrpcPort();

    this.proxyHandler = new ReloadableGrpcProxyHandler(logger, this, targetHost, targetPort);

    try {
      proxyServer =
          ServerBuilder.forPort(settings.getProxyGrpcPort())
              .fallbackHandlerRegistry(new GrpcProxyHandlerRegistry(logger, proxyHandler))
              .build()
              .start();

      logger.info(
          "🚀 GRPC proxy server started on port "
              + settings.getProxyGrpcPort()
              + " -> "
              + targetHost
              + ":"
              + targetPort);
    } catch (IOException e) {
      logger.error("Failed to start GRPC proxy server", e);
      throw new RuntimeException(e);
    }

    appThreadGroup = new ThreadGroup("app");
    isRunning.set(true);
  }

  @Override
  protected void prepareServerForNewGeneration() {
    // Refresh the proxy channel to the new instance
    proxyHandler.refreshChannel();
  }

  @Override
  protected void cleanupServerForOldGeneration() {
    // Close the proxy channel
    proxyHandler.closeChannel();
  }

  @Override
  protected void stopProxyServer() {
    logger.info("🛑 Stopping the GRPC application");
    if (proxyServer != null) {
      proxyServer.shutdownNow();
    }
  }
}
