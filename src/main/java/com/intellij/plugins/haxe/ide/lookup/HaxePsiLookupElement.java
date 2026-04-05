package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.plugins.haxe.HaxeComponentType;

public interface HaxePsiLookupElement extends HaxeLookupElement{
  HaxeComponentType getType();

  /**
   * A key to help with deduplication in completion suggestion list.
   *  This is normally the Qualified name of a component, however
   *  since we can have completion of both method referance and call expression
   *  we append some extra infomration
   * @return
   */
  String deduplicateKey();
}
