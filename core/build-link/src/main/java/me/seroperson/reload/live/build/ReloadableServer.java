package me.seroperson.reload.live.build;

import java.io.Closeable;

/** A server that can be reloaded or stopped. */
public interface ReloadableServer extends Closeable {

  void start();

  boolean isRunning();

  /** Reload the server if necessary. Returns true if was reloaded. */
  boolean reload();
}
