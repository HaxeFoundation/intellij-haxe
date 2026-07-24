package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/** One completion suggestion from the adapter's "completions" response. */
@Data
public class CompletionItem {
  private String label;
  /** The text to insert (label when absent). */
  private String text;
  /** DAP CompletionItemType: method/function/field/variable/property/... */
  private String type;
  private Integer start;
  private Integer length;
  private String sortText;
  private String detail;
}
