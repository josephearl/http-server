package uk.co.josephearl.http.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolHttpServer implements HttpServer {
  public static void main(String[] args) throws IOException {
    new ThreadPoolHttpServer().serve(3000);
  }

  @Override
  public void serve(int port) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port);
         ExecutorService executor = new ThreadPoolExecutor(1, 100, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10))) {
      boolean interrupted;

      while (!(interrupted = Thread.currentThread().isInterrupted())) {
        Socket clientSocket = serverSocket.accept();
        executor.submit(() -> {
          try (clientSocket;
               HttpConnection connection = new HttpConnection(clientSocket)) {
            connection.handle("Hello " + getClass().getSimpleName() + "!");
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }

      if (interrupted) {
        throw new InterruptedIOException("Interrupted while serving connections");
      }
    }
  }
}
