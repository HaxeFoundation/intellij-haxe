package com.intellij.plugins.haxe.runner.debugger.hxcpp.frame;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.HXCPPDebugProcess;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.connection.HXCPPResponse;
import com.intellij.util.io.socketConnection.AbstractResponseToRequestHandler;
import com.intellij.xdebugger.frame.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HXCPPValue extends XValue {
  private final String myName;
  private final HXCPPDebugProcess myDebugProcess;
  private final List<HXCPPValue> myFields = new ArrayList<HXCPPValue>();
  @Nullable
  private final String myValue;

  protected HXCPPValue(HXCPPDebugProcess debugProcess, String name) {
    this(debugProcess, name, null);
  }

  protected HXCPPValue(HXCPPDebugProcess debugProcess, String name, @Nullable String value) {
    myDebugProcess = debugProcess;
    myName = name;
    myValue = value;
  }

  public String getName() {
    return myName;
  }

  @Override
  public void computePresentation(@NotNull final XValueNode node, @NotNull XValuePlace place) {
    if (myValue != null) {
      node.setPresentation(AllIcons.Debugger.Value, "object", myValue, false);
      return;
    }
    myDebugProcess.sendCommand(new AbstractResponseToRequestHandler<HXCPPResponse>() {
      @Override
      public boolean processResponse(HXCPPResponse response) {
        String string = response.getResponseString();
        if (string.endsWith("ok")) {
          string = string.substring(0, string.length() - "ok".length());
        }
        string = StringUtil.trimTrailing(string);
        string = StringUtil.trimLeading(string);
        final List<Pair<String, String>> pairs = parseFields(string);
        if (pairs.isEmpty()) {
          node.setPresentation(AllIcons.Debugger.Value, "object", string, false);
        }
        else {
          for (Pair<String, String> pair : pairs) {
            myFields.add(new HXCPPValue(myDebugProcess, pair.getFirst(), pair.getSecond()));
          }
          node.setPresentation(AllIcons.Debugger.Value, "object", "{object}", true);
        }
        return true;
      }
    }, "p", myName);
  }

  private static List<Pair<String, String>> parseFields(String string) {
    if (string.indexOf('\t') == -1) return Collections.emptyList();
    final List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
    for (String data : string.split("\t")) {
      final int index = data.indexOf('=');
      if (index > 0) {
        result.add(Pair.create(data.substring(0, index - 1), data.substring(index + 1)));
      }
    }
    return result;
  }

  @Override
  public void computeChildren(@NotNull final XCompositeNode node) {
    final XValueChildrenList childrenList = new XValueChildrenList(myFields.size());
    for (HXCPPValue field : myFields) {
      childrenList.add(field.getName(), field);
    }
    node.addChildren(childrenList, true);
  }
}
