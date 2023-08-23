package uk.co.josephearl.http.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleHttpServer implements HttpServer {
  public static void main(String[] args) throws IOException {
    new SimpleHttpServer().serve(3000);
  }

  @Override
  public void serve(int port) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      boolean interrupted;

      while (!(interrupted = Thread.currentThread().isInterrupted())) {
        try (Socket clientSocket = serverSocket.accept();
             HttpConnection connection = new HttpConnection(clientSocket)) {
          connection.handle("Hello " + getClass().getSimpleName() + "!");
        }
      }

      if (interrupted) {
        throw new InterruptedIOException("Interrupted while serving connections");
      }
    }
  }
}
