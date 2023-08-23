package uk.co.josephearl.http.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiThreadHttpServer implements HttpServer {
  public static void main(String[] args) throws IOException {
    new MultiThreadHttpServer().serve(3000);
  }

  @Override
  public void serve(int port) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      boolean interrupted;

      while (!(interrupted = Thread.currentThread().isInterrupted())) {
        Socket clientSocket = serverSocket.accept();
        Thread thread = new Thread(() -> {
          try (clientSocket;
               HttpConnection connection = new HttpConnection(clientSocket)) {
            connection.handle("Hello " + getClass().getSimpleName() + "!");
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
        thread.start();
      }

      if (interrupted) {
        throw new InterruptedIOException("Interrupted while serving connections");
      }
    }
  }
}
