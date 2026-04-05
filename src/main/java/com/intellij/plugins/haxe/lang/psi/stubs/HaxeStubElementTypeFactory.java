package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.psi.tree.IElementType;

/**
 * Factory called by Grammar-Kit generated code to create element types.
 * For stub-worthy rules, returns the pre-created IStubElementType singletons from HaxeStubElementTypes.
 * For all other rules, returns a plain HaxeElementType.
 */
public class HaxeStubElementTypeFactory {

  public static IElementType createElement(String name) {
    return switch (name) {
      // Type declarations
      case "CLASS_DECLARATION" -> HaxeStubElementTypes.CLASS_DECLARATION;
      case "INTERFACE_DECLARATION" -> HaxeStubElementTypes.INTERFACE_DECLARATION;
      case "ENUM_DECLARATION" -> HaxeStubElementTypes.ENUM_DECLARATION;
      case "ABSTRACT_TYPE_DECLARATION" -> HaxeStubElementTypes.ABSTRACT_TYPE_DECLARATION;
      case "TYPEDEF_DECLARATION" -> HaxeStubElementTypes.TYPEDEF_DECLARATION;
      case "EXTERN_CLASS_DECLARATION" -> HaxeStubElementTypes.EXTERN_CLASS_DECLARATION;
      case "EXTERN_INTERFACE_DECLARATION" -> HaxeStubElementTypes.EXTERN_INTERFACE_DECLARATION;
      // Macro type declarations
      case "MACRO_CLASS_DECLARATION" -> HaxeStubElementTypes.MACRO_CLASS_DECLARATION;
      case "MACRO_INTERFACE_DECLARATION" -> HaxeStubElementTypes.MACRO_INTERFACE_DECLARATION;
      case "MACRO_ENUM_DECLARATION" -> HaxeStubElementTypes.MACRO_ENUM_DECLARATION;
      case "MACRO_ABSTRACT_TYPE_DECLARATION" -> HaxeStubElementTypes.MACRO_ABSTRACT_TYPE_DECLARATION;
      case "MACRO_TYPEDEF_DECLARATION" -> HaxeStubElementTypes.MACRO_TYPEDEF_DECLARATION;
      case "MACRO_EXTERN_CLASS_DECLARATION" -> HaxeStubElementTypes.MACRO_EXTERN_CLASS_DECLARATION;
      case "MACRO_EXTERN_INTERFACE_DECLARATION" -> HaxeStubElementTypes.MACRO_EXTERN_INTERFACE_DECLARATION;
      // Member declarations
      case "METHOD_DECLARATION" -> HaxeStubElementTypes.METHOD_DECLARATION;
      case "CONSTRUCTOR_DECLARATION" -> HaxeStubElementTypes.CONSTRUCTOR_DECLARATION;
      case "FIELD_DECLARATION" -> HaxeStubElementTypes.FIELD_DECLARATION;
      case "OPTIONAL_FIELD_DECLARATION" -> HaxeStubElementTypes.OPTIONAL_FIELD_DECLARATION;
      case "ENUM_VALUE_DECLARATION_CONSTRUCTOR" -> HaxeStubElementTypes.ENUM_VALUE_DECLARATION_CONSTRUCTOR;
      case "ENUM_VALUE_DECLARATION_FIELD" -> HaxeStubElementTypes.ENUM_VALUE_DECLARATION_FIELD;
      // Module-level members
      case "MODULE_FIELD_DECLARATION" -> HaxeStubElementTypes.MODULE_FIELD_DECLARATION;
      case "MODULE_METHOD_DECLARATION" -> HaxeStubElementTypes.MODULE_METHOD_DECLARATION;
      // Structure
      case "MODULE" -> HaxeStubElementTypes.MODULE;
      // Import / Using
      case "IMPORT_STATEMENT" -> HaxeStubElementTypes.IMPORT_STATEMENT;
      case "USING_STATEMENT" -> HaxeStubElementTypes.USING_STATEMENT;
      // Package
      case "PACKAGE_STATEMENT" -> HaxeStubElementTypes.PACKAGE_STATEMENT;
      // All other rules — plain element type
      default -> new HaxeElementType(name);
    };
  }
}

