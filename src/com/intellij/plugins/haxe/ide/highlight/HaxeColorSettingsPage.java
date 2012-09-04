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
 * @author fedor.korotkov
 */
public class HaxeColorSettingsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] ATTRS = new AttributesDescriptor[]{
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.line.comment"), LINE_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.block.comment"), BLOCK_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.doc.comment"), DOC_COMMENT),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation"), CONDITIONALLY_NOT_COMPILED),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation.defined.flag"), DEFINED_VAR),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.conditional.compilation.undefined.flag"), UNDEFINED_VAR),
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
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.instance.member.function"), INSTANCE_MEMBER_FUNCTION),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.static.member.function"), STATIC_MEMBER_FUNCTION),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.instance.member.variable"), INSTANCE_MEMBER_VARIABLE),
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.static.member.variable"), STATIC_MEMBER_VARIABLE)
  };

  @NonNls private static final Map<String, TextAttributesKey> ourTags = new HashMap<String, TextAttributesKey>();

  static {
    ourTags.put("parameter", PARAMETER);
    ourTags.put("local.variable", LOCAL_VARIABLE);
    ourTags.put("class", CLASS);
    ourTags.put("compilation", CONDITIONALLY_NOT_COMPILED);
    ourTags.put("defined.flag", DEFINED_VAR);
    ourTags.put("undefined.flag", UNDEFINED_VAR);
    ourTags.put("interface", INTERFACE);
    ourTags.put("instance.member.function", INSTANCE_MEMBER_FUNCTION);
    ourTags.put("static.member.function", STATIC_MEMBER_FUNCTION);
    ourTags.put("instance.member.variable", INSTANCE_MEMBER_VARIABLE);
    ourTags.put("static.member.variable", STATIC_MEMBER_VARIABLE);
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.title");
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HaXe_16;
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
           "      var <local.variable>a</local.variable>:<class>Int</class> = $$$;// bad character\n" +
           "    };\n" +
           "  }\n" +
           "}";
  }
}
