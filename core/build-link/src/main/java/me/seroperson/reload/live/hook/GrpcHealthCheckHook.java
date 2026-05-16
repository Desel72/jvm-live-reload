package me.seroperson.reload.live.hook;

import java.io.IOException;
import java.net.Socket;
import me.seroperson.reload.live.build.BuildLogger;

/**
 * Health check hook that uses GRPC health checking to determine server health.
 *
 * <p>This hook performs health checks by connecting to the GRPC server and checking if it responds.
 * It uses a simple TCP connection check, as GRPC health checking protocol would require having the
 * GRPC dependencies which are not available in the build-link module.
 */
interface GrpcHealthCheckHook extends HealthCheckHook {

  @Override
  default int isHealthy(BuildLogger logger, String path, String host, int port) {
    // For GRPC, we do a simple TCP connection check
    // The 'path' parameter is ignored for GRPC - it's the service name for GRPC health check
    try (Socket socket = new Socket(host, port)) {
      socket.setSoTimeout(500);
      // If we can connect, the server is at least accepting connections
      // A more sophisticated check would use the GRPC health checking protocol,
      // but that requires GRPC dependencies
      return 1;
    } catch (IOException e) {
      // Connection failed - server is not ready
      return -1;
    } catch (Exception e) {
      logger.error("Error during GRPC health check", e);
      return -1;
    }
  }
}
