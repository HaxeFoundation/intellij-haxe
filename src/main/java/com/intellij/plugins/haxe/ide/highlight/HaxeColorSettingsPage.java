/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Color Settings page for Haxe: Settings->Editor->Color Scheme->Haxe
 *
 * @author fedor.korotkov
 */
public class HaxeColorSettingsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = new AttributesDescriptor[]{
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.line.comment"), HaxeSyntaxHighlighterColors.LINE_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.block.comment"), HaxeSyntaxHighlighterColors.BLOCK_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.doc.comment"), HaxeSyntaxHighlighterColors.DOC_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation"), HaxeSyntaxHighlighterColors.CONDITIONALLY_NOT_COMPILED),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.unparseable.data"), HaxeSyntaxHighlighterColors.UNPARSEABLE_DATA),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation.defined.flag"), HaxeSyntaxHighlighterColors.DEFINED_VAR),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation.undefined.flag"), HaxeSyntaxHighlighterColors.UNDEFINED_VAR),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.metadata"), HaxeSyntaxHighlighterColors.METADATA),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.keyword"), HaxeSyntaxHighlighterColors.KEYWORD),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.number"), HaxeSyntaxHighlighterColors.NUMBER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.string"), HaxeSyntaxHighlighterColors.STRING),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.operator"), HaxeSyntaxHighlighterColors.OPERATION_SIGN),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.parenths"), HaxeSyntaxHighlighterColors.PARENTHS),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.brackets"), HaxeSyntaxHighlighterColors.BRACKETS),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.braces"), HaxeSyntaxHighlighterColors.BRACES),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.comma"), HaxeSyntaxHighlighterColors.COMMA),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.dot"), HaxeSyntaxHighlighterColors.DOT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.semicolon"), HaxeSyntaxHighlighterColors.SEMICOLON),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.bad.character"), HaxeSyntaxHighlighterColors.BAD_CHARACTER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.parameter"), HaxeSyntaxHighlighterColors.PARAMETER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.local.variable"), HaxeSyntaxHighlighterColors.LOCAL_VARIABLE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.class"), HaxeSyntaxHighlighterColors.CLASS),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.interface"), HaxeSyntaxHighlighterColors.INTERFACE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.instance.member.function"), HaxeSyntaxHighlighterColors.INSTANCE_MEMBER_FUNCTION),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.static.member.function"), HaxeSyntaxHighlighterColors.STATIC_MEMBER_FUNCTION),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.instance.member.variable"), HaxeSyntaxHighlighterColors.INSTANCE_MEMBER_VARIABLE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.static.member.variable"), HaxeSyntaxHighlighterColors.STATIC_MEMBER_VARIABLE)
  };

  @NonNls private static final Map<String, TextAttributesKey> ourTags = new HashMap<String, TextAttributesKey>();

  /* These strings define what token will be highlighted and selected when the code
     screen is focused/clicked upon in the settings dialog.
   */
  static {
    ourTags.put("parameter", HaxeSyntaxHighlighterColors.PARAMETER);
    ourTags.put("local.variable", HaxeSyntaxHighlighterColors.LOCAL_VARIABLE);
    ourTags.put("class", HaxeSyntaxHighlighterColors.CLASS);
    ourTags.put("compilation", HaxeSyntaxHighlighterColors.CONDITIONALLY_NOT_COMPILED);
    ourTags.put("unparseable", HaxeSyntaxHighlighterColors.UNPARSEABLE_DATA);
    ourTags.put("defined.flag", HaxeSyntaxHighlighterColors.DEFINED_VAR);
    ourTags.put("undefined.flag", HaxeSyntaxHighlighterColors.UNDEFINED_VAR);
    ourTags.put("interface", HaxeSyntaxHighlighterColors.INTERFACE);
    ourTags.put("instance.member.function", HaxeSyntaxHighlighterColors.INSTANCE_MEMBER_FUNCTION);
    ourTags.put("static.member.function", HaxeSyntaxHighlighterColors.STATIC_MEMBER_FUNCTION);
    ourTags.put("instance.member.variable", HaxeSyntaxHighlighterColors.INSTANCE_MEMBER_VARIABLE);
    ourTags.put("static.member.variable", HaxeSyntaxHighlighterColors.STATIC_MEMBER_VARIABLE);
    ourTags.put("metadata", HaxeSyntaxHighlighterColors.METADATA);
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.title");
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HAXE_LOGO;
  }

  @NotNull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new HaxeSyntaxHighlighter(null);
  }

  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ourTags;
  }

  @NotNull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @NotNull
  @Override
  public String getDemoText() {
    return "<compilation>#if <defined.flag>definedFlag</defined.flag> && <undefined.flag>undefinedFlag</undefined.flag>\n" +
           "#error \"Error!!\"\n" +
           "#else</compilation>\n" +
           "import <class>util.Date</class>;\n" +
           "<compilation>#end</compilation>\n" +
           "\n" +
           "/* Block comment */\n" +
           "/**\n" +
           " Document comment\n" +
           "**/\n" +
           "@author(\"Penelope\")\n" +
           "@:final\n" +
           "class <class>SomeClass</class> implements <interface>IOther</interface> { // some comment\n" +
           "  private var <instance.member.variable>field</instance.member.variable> = null;\n" +
           "  private var <instance.member.variable>unusedField</instance.member.variable>:<class>Number</class> = 12345.67890;\n" +
           "  private var <instance.member.variable>anotherString</instance.member.variable>:<class>String</class> = \"Another\\nStrin\\g\";\n" +
           "  public static var <static.member.variable>staticField</static.member.variable>:<class>Int</class> = 0;\n" +
           "\n" +
           "  public static function <static.member.function>inc</static.member.function>() {\n" +
           "    <static.member.variable>staticField</static.member.variable>++;\n" +
           "  }\n" +
           "  public function <instance.member.function>foo</instance.member.function>(<parameter>param</parameter>:<interface>AnInterface</interface>) {\n" +
           "    trace(<instance.member.variable>anotherString</instance.member.variable> + <parameter>param</parameter>);\n" +
           "    var <local.variable>reassignedValue</local.variable>:<class>Int</class> = <class>SomeClass</class>.<static.member.variable>staticField</static.member.variable>; \n" +
           "    <local.variable>reassignedValue</local.variable> ++; \n" +
           "    function localFunction() {\n" +
           "      var <local.variable>a</local.variable>:<class>Int</class> = \\?;// bad character `\\` \n" +
           "    };\n" +
           "  }\n" +
           "}\n" +
           "/* The next line is deliberately invalid syntax to show unparsable data. */\n" +
           "<unparseable>var $.{}{}</unparseable>"
           ;
  }
}
