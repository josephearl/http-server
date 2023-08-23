package uk.co.josephearl.http.server;

public class AsyncHttpConnection {
  public static abstract sealed class ConnectionState {
    static final class Read extends ConnectionState {
      public final byte[] request = new byte[1024 * 2];
      public int read = 0;
    }

    static final class Write extends ConnectionState {
      public final byte[] response;
      public int written;

      public Write(byte[] response) {
        this.response = response;
      }
    }
  }
}
