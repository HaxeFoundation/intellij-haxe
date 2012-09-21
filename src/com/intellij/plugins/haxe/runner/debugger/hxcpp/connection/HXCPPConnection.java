package com.intellij.plugins.haxe.runner.debugger.hxcpp.connection;

import com.intellij.openapi.Disposable;
import com.intellij.util.io.socketConnection.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * @author: Fedor.Korotkov
 */
public class HXCPPConnection {
  private SocketConnection<HXCPPCommand, HXCPPResponse> myConnection;
  private int lastCommandId = 0;

  public void connect(int debuggingPort) {
    final HXCPPRequestReaderWriter readerWrite = new HXCPPRequestReaderWriter();
    myConnection = SocketConnectionFactory.createServerConnection(
      debuggingPort,
      new RequestResponseExternalizerFactory<HXCPPCommand, HXCPPResponse>() {
        @NotNull
        @Override
        public RequestWriter<HXCPPCommand> createRequestWriter(@NotNull OutputStream output)
          throws IOException {
          //noinspection IOResourceOpenedButNotSafelyClosed
          readerWrite.setWriter(new OutputStreamWriter(output, "UTF-8"));
          return readerWrite;
        }

        @NotNull
        @Override
        public ResponseReader<HXCPPResponse> createResponseReader(@NotNull InputStream input)
          throws IOException {
          //noinspection IOResourceOpenedButNotSafelyClosed
          readerWrite.setReader(new BufferedReader(new InputStreamReader(input, "UTF-8")));
          return readerWrite;
        }
      });
  }

  public synchronized void sendCommand(@Nullable AbstractResponseToRequestHandler<HXCPPResponse> responseHandler, String... args) {
    final int id = responseHandler == null ? -1 : lastCommandId++;
    myConnection.sendRequest(new HXCPPCommand(id, args), responseHandler);
  }

  public void close() {
    myConnection.close();
  }

  public void open() throws IOException {
    myConnection.open();
  }

  public void registerHandler(AbstractResponseHandler<HXCPPResponse> debugProcess) {
    myConnection.registerHandler(HXCPPResponse.class, debugProcess);
  }

  public void addListener(@NotNull SocketConnectionListener listener){
    myConnection.addListener(listener, null);
  }
}
