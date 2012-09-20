package com.intellij.plugins.haxe.runner.debugger.hxcpp.connection;

import com.intellij.util.io.socketConnection.AbstractResponse;

public class HXCPPResponse implements AbstractResponse {

  private final String myResponseString;

  public HXCPPResponse(String responseString) {
    myResponseString = responseString;
  }

  public String getResponseString() {
    return myResponseString;
  }

  @Override
  public String toString() {
    return "HXCPPResponse{" +
           "myResponseStrings=" + myResponseString +
           '}';
  }
}
