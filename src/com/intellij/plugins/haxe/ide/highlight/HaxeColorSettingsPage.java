package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
    new AttributesDescriptor(HaxeBundle.message("haxe.color.settings.description.bad.character"), BAD_CHARACTER)
  };

  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.title");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  @NotNull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public SyntaxHighlighter getHighlighter() {
    return new HaxeSyntaxHighlighter();
  }

  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @NotNull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @NotNull
  @Override
  public String getDemoText() {
    return "/* Block comment */\n" +
           "import java.util.Date;\n" +
           "\n" +
           "/**\n" +
           " Document comment\n" +
           "**/\n" +
           "class SomeClass { // some comment\n" +
           "  private var field = null;\n" +
           "  private var unusedField:Number = 12345.67890;\n" +
           "  private var anotherString:String = \"Another\\nStrin\\g\";\n" +
           "  public static var staticField:Int = 0;\n" +
           "\n" +
           "  public function new(param:AnInterface, reassignedParam:Array) {\n" +
           "    trace(anotherString + field);\n" +
           "    var reassignedValue:Int = this.staticField; \n" +
           "    reassignedValue ++; \n" +
           "    function localFunction() {\n" +
           "      var a:Int = $$$;// bad character\n" +
           "    };\n" +
           "  }\n" +
           "}";
  }
}
