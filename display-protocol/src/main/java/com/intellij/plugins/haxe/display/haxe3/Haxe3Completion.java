package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.haxe3.Haxe3Wire.FieldEntry;
import com.intellij.plugins.haxe.display.haxe3.Haxe3Wire.LocalToplevel;
import com.intellij.plugins.haxe.display.haxe3.Haxe3Wire.ToplevelEntry;
import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.CompletionList;
import com.intellij.plugins.haxe.display.transport.MalformedPayloadException;
import java.util.ArrayList;
import java.util.List;

/**
 * Completion over haxe 3's two answers. After a dot, the field mode
 * ({@code <file>@<offset>}, the offset right after the dot) lists a value's
 * fields, or the types and packages of a package path ({@link FieldEntry}).
 * Anywhere else the {@code @toplevel} mode lists what is in scope
 * ({@link ToplevelEntry}). Neither carries a replace range, an expected type
 * or keywords.
 */
final class Haxe3Completion {

  // the haxe 4 completion modes the two answers correspond to
  private static final int FIELD_MODE = 0;
  private static final int TOPLEVEL_MODE = 2;

  /** The answer when haxe 3 finds no completion point at the position. */
  static final CompletionList NOTHING = new CompletionList(List.of(), TOPLEVEL_MODE, null, null, false);

  /**
   * Where and how to ask: the request offset, whether it is a field access,
   * the partial name the caret ends, and for a field access the dotted path
   * before its dot.
   */
  record Query(int requestOffset, boolean fieldAccess, String partialName, String qualifier) {

    static Query at(String text, int offset) {
      int nameStart = Haxe3Offsets.identifierStart(text, offset);
      String partialName = text.substring(nameStart, Math.clamp(offset, nameStart, text.length()));
      boolean fieldAccess = nameStart > 0 && text.charAt(nameStart - 1) == '.';
      if (!fieldAccess) return new Query(offset, false, partialName, "");
      int dot = nameStart - 1;
      String qualifier = text.substring(Haxe3Offsets.dottedChainStart(text, dot), dot);
      return new Query(nameStart, true, partialName, qualifier);
    }
  }

  private Haxe3Completion() {
  }

  /** The items of a field-mode answer. */
  static CompletionList fieldList(String payload, Query query) throws MalformedPayloadException {
    List<CompletionItem> items = new ArrayList<>();
    for (FieldEntry entry : Haxe3Xml.read(payload).fields()) {
      CompletionItem item = entry.toItem(query.qualifier(), items.size());
      if (item != null) items.add(item);
    }
    return new CompletionList(List.copyOf(items), FIELD_MODE, null, null, false);
  }

  /** The items of a toplevel answer, without the entry haxe 3 makes of the partial name itself. */
  static CompletionList toplevelList(String payload, Query query) throws MalformedPayloadException {
    List<CompletionItem> items = new ArrayList<>();
    for (ToplevelEntry entry : Haxe3Xml.read(payload).toplevelEntries()) {
      if (entry instanceof LocalToplevel local && local.isPartialName(query.partialName())) continue;
      CompletionItem item = entry.toItem(items.size());
      if (item != null) items.add(item);
    }
    return new CompletionList(List.copyOf(items), TOPLEVEL_MODE, null, null, false);
  }
}
