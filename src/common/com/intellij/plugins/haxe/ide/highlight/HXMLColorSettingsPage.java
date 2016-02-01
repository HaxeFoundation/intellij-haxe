/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class HXMLColorSettingsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
    new AttributesDescriptor("Key", HXMLSyntaxHighlighter.KEY),
    new AttributesDescriptor("Value", HXMLSyntaxHighlighter.VALUE),
    new AttributesDescriptor("Comment", HXMLSyntaxHighlighter.COMMENT),
    new AttributesDescriptor("Include", HXMLSyntaxHighlighter.INCLUDE),
    new AttributesDescriptor("ClassName", HXMLSyntaxHighlighter.CLASS_NAME),
  };

  @Nullable
  @Override
  public Icon getIcon() {
    return HaxeIcons.Haxe_16;
  }

  @NotNull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new HXMLSyntaxHighlighter();
  }

  @NotNull
  @Override
  public String getDemoText() {
    return "build-each.hxml\n" +
           "\n" +
           "-main test.Test\n" +
           "--each\n" +
           "\n" +
           "-neko bin/Test.n\n" +
           "-cmd neko bin/Test.n\n" +
           "\n" +
           "# --next\n" +
           "# -swf bin/Test.swf\n" +
           "# -cmd open bin/Test.swf\n" +
           "\n" +
           "test.pack.ForceImportMe\n";
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @NotNull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @NotNull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "HXML";
  }
}
