package com.intellij.plugins.haxe.ide.inspections.buildfiles;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** How the build-file inspections address XML attribute values: the {@code name} attribute's value, and the text inside the quotes. */
final class HaxeXmlAttributeValues {

  private HaxeXmlAttributeValues() {
  }

  @Nullable
  static XmlAttributeValue nameAttributeValue(@NotNull XmlTag tag) {
    XmlAttribute attribute = tag.getAttribute("name");
    return attribute == null ? null : attribute.getValueElement();
  }

  /** The value's text without its quotes, as a range inside the value element. */
  @NotNull
  static TextRange valueRange(@NotNull XmlAttributeValue value) {
    return value.getValueTextRange().shiftLeft(value.getTextRange().getStartOffset());
  }
}
