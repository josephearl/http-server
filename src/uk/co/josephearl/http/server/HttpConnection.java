package uk.co.josephearl.http.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

public class HttpConnection implements Closeable {
  private final Socket clientSocket;

  public HttpConnection(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  public void handle(String message) throws IOException {
    try (InputStream inputStream = clientSocket.getInputStream(); OutputStream outputStream = clientSocket.getOutputStream()) {
      readRequest(inputStream);
      writeResponse(outputStream, message);
      flush(outputStream);
    }
  }

  @Override
  public void close() throws IOException {
    clientSocket.close();
  }

  private void readRequest(InputStream inputStream) throws IOException {
    int read = 0;
    byte[] request = new byte[1024];
    boolean interrupted;

    while (!(interrupted = Thread.currentThread().isInterrupted())) {
      int bytesRead = inputStream.read(request, read, 1024 - read);

      // Client disconnected
      if (bytesRead == 0) {
        throw new IOException("Client disconnected");
      }

      read += bytesRead;

      // Have we read the end of the request? \r\n\r\n
      if (read >= 4
        && request[read - 4] == '\r'
        && request[read - 3] == '\n'
        && request[read - 2] == '\r'
        && request[read - 1] == '\n') {
        return;
      }
    }

    if (interrupted) {
      throw new InterruptedIOException("Interrupted while reading request");
    }
  }

  private void writeResponse(OutputStream outputStream, String message) throws IOException {
    int statusCode = 200;
    String statusCodeMeaning = "OK";
    String responseString
      = "HTTP/1.1 " + statusCode + " " + statusCodeMeaning + "\r\n"
      + "Content-Length: " + message.getBytes().length + "\r\n"
      + "\r\n"
      + message;
    byte[] response = responseString.getBytes();

    outputStream.write(response);
  }

  private void flush(OutputStream outputStream) throws IOException {
    outputStream.flush();
  }
}
