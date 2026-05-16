package me.seroperson.reload.live.webserver.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import me.seroperson.reload.live.build.BuildLogger;

/**
 * Manages a reloadable GRPC channel that can be refreshed when the application is reloaded.
 *
 * <p>This handler maintains a connection to the target GRPC server and provides the ability to
 * close and recreate the channel when the application is reloaded, ensuring that the proxy always
 * connects to the latest version of the application.
 */
class ReloadableGrpcProxyHandler {

  private final BuildLogger logger;
  private final GrpcDevServerStart server;
  private final String targetHost;
  private final int targetPort;
  private final AtomicReference<ManagedChannel> channelRef = new AtomicReference<>();

  /**
   * Creates a new reloadable GRPC proxy handler.
   *
   * @param logger the logger for outputting messages
   * @param server the dev server for triggering reloads
   * @param targetHost the host of the target GRPC server
   * @param targetPort the port of the target GRPC server
   */
  public ReloadableGrpcProxyHandler(
      BuildLogger logger, GrpcDevServerStart server, String targetHost, int targetPort) {
    this.logger = logger;
    this.server = server;
    this.targetHost = targetHost;
    this.targetPort = targetPort;
  }

  /**
   * Gets the current channel, creating one if necessary.
   *
   * @return the managed channel to the target server
   */
  public Channel getChannel() {
    ManagedChannel channel = channelRef.get();
    if (channel == null || channel.isShutdown() || channel.isTerminated()) {
      channel = createChannel();
      channelRef.set(channel);
    }
    return channel;
  }

  /**
   * Creates a new client call to the target server.
   *
   * @param methodDescriptor the method to call
   * @param callOptions the call options
   * @param <ReqT> the request type
   * @param <RespT> the response type
   * @return a new client call
   */
  public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions) {
    return getChannel().newCall(methodDescriptor, callOptions);
  }

  /** Refreshes the channel by closing the existing one and creating a new one. */
  public void refreshChannel() {
    closeChannel();
    logger.debug("Refreshing GRPC channel to " + targetHost + ":" + targetPort);
    channelRef.set(createChannel());
  }

  /** Closes the current channel if it exists. */
  public void closeChannel() {
    ManagedChannel channel = channelRef.getAndSet(null);
    if (channel != null && !channel.isShutdown()) {
      logger.debug("Closing GRPC channel");
      channel.shutdown();
      try {
        if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
          channel.shutdownNow();
        }
      } catch (InterruptedException e) {
        channel.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  public boolean reload() {
    return server.reload();
  }

  private ManagedChannel createChannel() {
    logger.debug("Creating new GRPC channel to " + targetHost + ":" + targetPort);
    return ManagedChannelBuilder.forAddress(targetHost, targetPort).usePlaintext().build();
  }

  /**
   * Gets the target host.
   *
   * @return the target host
   */
  public String getTargetHost() {
    return targetHost;
  }

  /**
   * Gets the target port.
   *
   * @return the target port
   */
  public int getTargetPort() {
    return targetPort;
  }
}
