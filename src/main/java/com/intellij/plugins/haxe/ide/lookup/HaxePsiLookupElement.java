package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.plugins.haxe.HaxeComponentType;
import org.jspecify.annotations.NonNull;

public interface HaxePsiLookupElement extends HaxeLookupElement {
  HaxeComponentType getType();

  /**
   * A key to help with deduplication of the completion suggestion-list.
   * This is normally the Qualified name of a component, however,
   * since we can have completion of both method reference and callExpression
   * we append some extra information
   *
   * @return
   */
  String deduplicateKey();
}
