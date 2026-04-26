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

      case "ANONYMOUS_TYPE" -> HaxeStubElementTypes.ANONYMOUS_TYPE;
      case "ANONYMOUS_TYPE_BODY" -> HaxeStubElementTypes.ANONYMOUS_TYPE_BODY;
      case "TYPE_EXTENDS_LIST" -> HaxeStubElementTypes.TYPE_EXTENDS_LIST;
      case "ANONYMOUS_TYPE_FIELD_LIST" -> HaxeStubElementTypes.ANONYMOUS_TYPE_FIELD_LIST;
      case "ANONYMOUS_TYPE_FIELD" -> HaxeStubElementTypes.ANONYMOUS_TYPE_FIELD;
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
      // Component name
      case "COMPONENT_NAME" -> HaxeStubElementTypes.COMPONENT_NAME;
      // Reference expression (stub for use in type sub-tree)
      case "REFERENCE_EXPRESSION" -> HaxeStubElementTypes.REFERENCE_EXPRESSION;
      // Type sub-tree (container stubs)
      case "TYPE_TAG" -> HaxeStubElementTypes.TYPE_TAG;
      case "TYPE_OR_ANONYMOUS" -> HaxeStubElementTypes.TYPE_OR_ANONYMOUS;
      case "TYPE" -> HaxeStubElementTypes.TYPE;
      case "TYPE_PARAM" -> HaxeStubElementTypes.TYPE_PARAM;
      case "TYPE_LIST_PART" -> HaxeStubElementTypes.TYPE_LIST_PART;
      case "FUNCTION_TYPE" -> HaxeStubElementTypes.FUNCTION_TYPE;
      case "FUNCTION_ARGUMENT" -> HaxeStubElementTypes.FUNCTION_ARGUMENT;
      case "FUNCTION_RETURN_TYPE" -> HaxeStubElementTypes.FUNCTION_RETURN_TYPE;
      case "REST_ARGUMENT_TYPE" -> HaxeStubElementTypes.REST_ARGUMENT_TYPE;
      case "OLD_REST_ARGUMENT_TYPE" -> HaxeStubElementTypes.OLD_REST_ARGUMENT_TYPE;
      // Parameters
      case "PARAMETER" -> HaxeStubElementTypes.PARAMETER;
      case "REST_PARAMETER" -> HaxeStubElementTypes.REST_PARAMETER;
      case "UNTYPED_PARAMETER" -> HaxeStubElementTypes.UNTYPED_PARAMETER;
      case "PARAMETER_LIST" -> HaxeStubElementTypes.PARAMETER_LIST;
      case "OPEN_PARAMETER_LIST" -> HaxeStubElementTypes.OPEN_PARAMETER_LIST;
      // Generic type params
      case "GENERIC_LIST_PART" -> HaxeStubElementTypes.GENERIC_LIST_PART;
      case "GENERIC_PARAM" -> HaxeStubElementTypes.GENERIC_PARAM;
      case "GENERIC_DEFAULT_TYPE" -> HaxeStubElementTypes.GENERIC_DEFAULT_TYPE;
      case "GENERIC_CONSTRAINT_PART" -> HaxeStubElementTypes.GENERIC_CONSTRAINT_PART;
      //inheritance
      case "INHERIT_LIST" -> HaxeStubElementTypes.INHERIT_LIST;
      case "EXTENDS_DECLARATION" -> HaxeStubElementTypes.EXTENDS_DECLARATION;
      case "IMPLEMENTS_DECLARATION" -> HaxeStubElementTypes.IMPLEMENTS_DECLARATION;
      // All other rules — plain element type
      default -> new HaxeElementType(name);
    };
  }
}

