package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.CompletionItem;
import java.util.List;
import lombok.Data;

@Data
public class CompletionsResponseBody {
  private List<CompletionItem> targets;
}
