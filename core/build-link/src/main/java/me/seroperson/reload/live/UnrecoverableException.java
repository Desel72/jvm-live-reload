package me.seroperson.reload.live;

public class UnrecoverableException extends RuntimeException {

  public UnrecoverableException(String message, Throwable t) {
    super(message, t);
  }

  public UnrecoverableException(String message) {
    super(message);
  }
}
