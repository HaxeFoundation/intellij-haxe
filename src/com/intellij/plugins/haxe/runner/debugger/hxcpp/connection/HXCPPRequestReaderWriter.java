package com.intellij.plugins.haxe.runner.debugger.hxcpp.connection;

import com.intellij.idea.LoggerFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.socketConnection.RequestWriter;
import com.intellij.util.io.socketConnection.ResponseReader;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

public class HXCPPRequestReaderWriter implements RequestWriter<HXCPPCommand>, ResponseReader<HXCPPResponse> {
  private static final Logger LOG = LoggerFactory.getInstance().getLoggerInstance(HXCPPRequestReaderWriter.class.getName());
  private Writer myWriter;
  private BufferedReader myReader;
  private int id = 0;
  private final Set<Integer> allowedIds = new THashSet<Integer>();

  public void setWriter(Writer writer) {
    myWriter = writer;
  }

  public void setReader(BufferedReader reader) {
    myReader = reader;
  }

  @Override
  public void writeRequest(HXCPPCommand request) throws IOException {
    if (request.getId() >= 0) {
      allowedIds.add(request.getId());
    }
    request.sendCommand(myWriter);
  }

  @Override
  public HXCPPResponse readResponse() throws IOException, InterruptedException {
    String result = readMessageBlocking();
    while (!StringUtil.containsAlphaCharacters(result)) {
      result = readMessageBlocking();
    }

    // "stopped." only on breaks. It isn't response.
    if (!result.contains("stopped.") && allowedIds.remove(id)) {
      LOG.debug("read response " + id + ":\n" + result);
      return new HXCPPResponseToRequest(id++, result);
    }
    LOG.debug("read response :\n" + result);
    return new HXCPPResponse(result);
  }

  private String readMessageBlocking() throws IOException {
    //waiting
    while (!myReader.ready()) {
      // ignore
    }
    //read all
    StringBuilder text = new StringBuilder();

    // hack
    int i1 = 0, i2 = 0, i3 = 0;

    while (myReader.ready()) {
      final int ch = myReader.read();
      // ignore strange spaces
      if (ch > 2) {
        text.append((char)ch);
      }

      // TODO: remove hack
      i1 = i2;
      i2 = i3;
      i3 = ch;
      if (i1 <= 2 && i2 == 'o' && i3 == 'k') {
        // \u0001ok is delimiter
        break;
      }
      if (ch == '.' && text.indexOf("stopped.") != -1) {
        break;
      }
    }

    return text.toString();
  }
}
