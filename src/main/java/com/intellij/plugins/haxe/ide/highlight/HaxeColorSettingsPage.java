/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019-2020 Eric Bishton
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

import static com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors.*;

/**
 * Color Settings page for Haxe: Settings->Editor->Color Scheme->Haxe
 *
 * @author fedor.korotkov
 */
public class HaxeColorSettingsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = new AttributesDescriptor[]{
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.line.comment"), LINE_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.block.comment"), BLOCK_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.doc.comment"), DOC_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation"), CONDITIONALLY_NOT_COMPILED),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.unparseable.data"), UNPARSEABLE_DATA),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation.defined.flag"), DEFINED_VAR),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation.undefined.flag"), UNDEFINED_VAR),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.metadata"), METADATA),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.keyword"), KEYWORD),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.number"), NUMBER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.string"), STRING),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.operator"), OPERATION_SIGN),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.parenths"), PARENTHS),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.brackets"), BRACKETS),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.braces"), BRACES),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.comma"), COMMA),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.dot"), DOT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.semicolon"), SEMICOLON),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.bad.character"), BAD_CHARACTER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.parameter"), PARAMETER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.local.variable"), LOCAL_VARIABLE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.class"), CLASS),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.interface"), INTERFACE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.type-parameter"), TYPE_PARAMETER),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.instance.member.function"), INSTANCE_MEMBER_FUNCTION),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.static.member.function"), STATIC_MEMBER_FUNCTION),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.instance.member.variable"), INSTANCE_MEMBER_VARIABLE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.static.member.variable"), STATIC_MEMBER_VARIABLE)
  };

  @NonNls private static final Map<String, TextAttributesKey> ourTags = new HashMap<String, TextAttributesKey>();

  /* These strings define what token will be highlighted and selected when the code
     screen is focused/clicked upon in the settings dialog.
   */
  static {
    ourTags.put("parameter", PARAMETER);
    ourTags.put("local.variable", LOCAL_VARIABLE);
    ourTags.put("class", CLASS);
    ourTags.put("compilation", CONDITIONALLY_NOT_COMPILED);
    ourTags.put("unparseable", UNPARSEABLE_DATA);
    ourTags.put("defined.flag", DEFINED_VAR);
    ourTags.put("undefined.flag", UNDEFINED_VAR);
    ourTags.put("interface", INTERFACE);
    ourTags.put("type.parameter", TYPE_PARAMETER);
    ourTags.put("instance.member.function", INSTANCE_MEMBER_FUNCTION);
    ourTags.put("static.member.function", STATIC_MEMBER_FUNCTION);
    ourTags.put("instance.member.variable", INSTANCE_MEMBER_VARIABLE);
    ourTags.put("static.member.variable", STATIC_MEMBER_VARIABLE);
    ourTags.put("metadata", METADATA);
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
    return """
      <compilation>#if <defined.flag>definedFlag</defined.flag> && <undefined.flag>undefinedFlag</undefined.flag>
      #error "Error!!"
      #else</compilation>
      import <class>util.Date</class>;
      <compilation>#end</compilation>
            
      /* Block comment */
      /**
       Document comment
      **/
      @author("Penelope")
      @:final
      class <class>SomeClass</class><<type.parameter>T</type.parameter>> implements <interface>IOther</interface> { // some comment
        private var <instance.member.variable>field</instance.member.variable> = null;
        private var <instance.member.variable>unusedField</instance.member.variable>:<class>Number</class> = 12345.67890;
        private var <instance.member.variable>anotherString</instance.member.variable>:<class>String</class> = "Another\\nStrin\\g";
        public static var <static.member.variable>staticField</static.member.variable>:<class>Int</class> = 0;
            
        public function generic<<type.parameter>K</type.parameter>:String>(arg:<type.parameter>K</type.parameter>):<type.parameter>K</type.parameter> return arg;
            
        public static function <static.member.function>inc</static.member.function>() {
          <static.member.variable>staticField</static.member.variable>++;
        }
        public function <instance.member.function>foo</instance.member.function>(<parameter>param</parameter>:<interface>AnInterface</interface>) {
          trace(<instance.member.variable>anotherString</instance.member.variable> + <parameter>param</parameter>);
          var <local.variable>reassignedValue</local.variable>:<class>Int</class> = <class>SomeClass</class>.<static.member.variable>staticField</static.member.variable>;\s
          <local.variable>reassignedValue</local.variable> ++;\s
          function localFunction() {
            var <local.variable>a</local.variable>:<class>Int</class> = \\?;// bad character `\\`\s
          };
        }
      }
      /* The next line is deliberately invalid syntax to show unparsable data. */
      <unparseable>var $.{}{}</unparseable>
      """;
  }
}
