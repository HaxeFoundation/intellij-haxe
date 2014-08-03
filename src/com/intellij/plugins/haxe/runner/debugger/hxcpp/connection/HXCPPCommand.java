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

import com.intellij.idea.LoggerFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.socketConnection.AbstractRequest;

import java.io.IOException;
import java.io.Writer;

public class HXCPPCommand implements AbstractRequest {
  private static final Logger LOG = Logger.getInstance(HXCPPCommand.class.getName());
  private final String[] myArgs;
  private final int myId;

  public HXCPPCommand(int id, String... args) {
    myId = id;
    myArgs = args;
  }

  @Override
  public int getId() {
    return myId;
  }

  public void sendCommand(Writer writer) throws IOException {
    StringBuilder result = new StringBuilder();
    for (String arg : myArgs) {
      if (result.length() > 0) {
        result.append(' ');
      }
      result.append(arg);
    }
    final String message = result.toString();
    LOG.debug("write for " + myId + ": " + message);
    writer.write(message + "\n");
    writer.flush();
  }
}
