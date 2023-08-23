package uk.co.josephearl.http.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AsyncHttpServer implements HttpServer {
  public static void main(String[] args) throws IOException {
    new AsyncHttpServer().serve(3000);
  }

  @Override
  public void serve(int port) throws IOException {
    try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
         Selector selector = Selector.open()) {
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", port));
      // Register to be notified of new connections
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
      // Keep track of active connections
      Map<SocketChannel, AsyncHttpConnection.ConnectionState> activeConnections = new LinkedHashMap<>();
      boolean interrupted;

      while (!(interrupted = Thread.currentThread().isInterrupted())) {
        selector.select();
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        List<SocketChannel> completed = new LinkedList<>();

        next: while (keys.hasNext()) {
          SelectionKey key = keys.next();
          // Remove the key so that we don't process this operation again
          keys.remove();

          // Connection closed
          if (!key.isValid()) {
            continue next;
          }

          // New connection
          if (key.isAcceptable()) {
            SocketChannel clientSocketChannel = serverSocketChannel.accept();
            clientSocketChannel.configureBlocking(false);
            clientSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            activeConnections.put(clientSocketChannel, new AsyncHttpConnection.ConnectionState.Read());
            continue next;
          }

          // Otherwise, a connection must be ready to read or write, try to make progress
          SocketChannel clientSocketChannel = (SocketChannel) key.channel();
          AsyncHttpConnection.ConnectionState connectionState = activeConnections.get(clientSocketChannel);
          switch (connectionState) {
            case AsyncHttpConnection.ConnectionState.Read r -> {
              ByteBuffer requestBuffer = ByteBuffer.allocate(r.request.length - r.read);
              int bytesRead = clientSocketChannel.read(requestBuffer);

              // Client disconnected
              if (bytesRead <= 0) {
                completed.add(clientSocketChannel);
                continue next;
              }

              requestBuffer.flip();
              requestBuffer.get(r.request, r.read, bytesRead);

              r.read += bytesRead;

              // Have we read the end of the request? \r\n\r\n
              if (r.read >= 4
                      && r.request[r.read - 4] == '\r'
                      && r.request[r.read - 3] == '\n'
                      && r.request[r.read - 2] == '\r'
                      && r.request[r.read - 1] == '\n') {
                // Transition to write state
                String message = "Hello " + getClass().getSimpleName() + "!";
                int statusCode = 200;
                String statusCodeMeaning = "OK";
                String responseString
                  = "HTTP/1.1 " + statusCode + " " + statusCodeMeaning + "\r\n"
                  + "Content-Length: " + message.getBytes().length + "\r\n"
                  + "\r\n"
                  + message;
                byte[] response = responseString.getBytes();
                activeConnections.put(clientSocketChannel, new AsyncHttpConnection.ConnectionState.Write(response));
              }
            }

            case AsyncHttpConnection.ConnectionState.Write w -> {
              ByteBuffer responseBuffer = ByteBuffer.wrap(w.response, w.written, w.response.length - w.written);
              int bytesWritten = clientSocketChannel.write(responseBuffer);

              w.written += bytesWritten;

              // Have we written the end of the response?
              if (w.written == w.response.length) {
                completed.add(clientSocketChannel);
              }
            }
          }
        }

        for (SocketChannel clientSocketChannel : completed) {
          activeConnections.remove(clientSocketChannel);
          clientSocketChannel.close();
        }
      }

      if (interrupted) {
        throw new InterruptedIOException("Interrupted while serving connections");
      }
    }
  }
}
