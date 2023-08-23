package uk.co.josephearl.http.server;

import java.io.IOException;

public interface HttpServer {
  void serve(int port) throws IOException;
}
