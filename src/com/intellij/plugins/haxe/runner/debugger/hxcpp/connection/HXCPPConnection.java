/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.runner.debugger.hxcpp.connection;

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

  public void addListener(@NotNull SocketConnectionListener listener) {
    myConnection.addListener(listener, null);
  }
}
