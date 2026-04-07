package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.lang.psi.stubs.type.*;

/**
 * Holder for all Haxe stub element type singletons.
 * Referenced by the element type factory and registered as a stubElementTypeHolder in plugin.xml.
 */
public interface HaxeStubElementTypes {

  // --- Type declarations ---

  HaxeClassStubElementType CLASS_DECLARATION = new HaxeClassStubElementType(
    "CLASS_DECLARATION", (stub, type) -> new HaxeClassDeclarationImpl(stub, type));
  HaxeClassStubElementType INTERFACE_DECLARATION = new HaxeClassStubElementType(
    "INTERFACE_DECLARATION", (stub, type) -> new HaxeInterfaceDeclarationImpl(stub, type));
  HaxeClassStubElementType ENUM_DECLARATION = new HaxeClassStubElementType(
    "ENUM_DECLARATION", (stub, type) -> new HaxeEnumDeclarationImpl(stub, type));
  HaxeClassStubElementType ABSTRACT_TYPE_DECLARATION = new HaxeClassStubElementType(
    "ABSTRACT_TYPE_DECLARATION", (stub, type) -> new HaxeAbstractTypeDeclarationImpl(stub, type));
  HaxeClassStubElementType TYPEDEF_DECLARATION = new HaxeClassStubElementType(
    "TYPEDEF_DECLARATION", (stub, type) -> new HaxeTypedefDeclarationImpl(stub, type));
  HaxeClassStubElementType EXTERN_CLASS_DECLARATION = new HaxeClassStubElementType(
    "EXTERN_CLASS_DECLARATION", (stub, type) -> new HaxeExternClassDeclarationImpl(stub, type));
  HaxeClassStubElementType EXTERN_INTERFACE_DECLARATION = new HaxeClassStubElementType(
    "EXTERN_INTERFACE_DECLARATION", (stub, type) -> new HaxeExternInterfaceDeclarationImpl(stub, type));

  // --- Macro type declarations ---
  HaxeClassStubElementType MACRO_CLASS_DECLARATION = new HaxeClassStubElementType(
    "MACRO_CLASS_DECLARATION", (stub, type) -> new HaxeMacroClassDeclarationImpl(stub, type));
  HaxeClassStubElementType MACRO_INTERFACE_DECLARATION = new HaxeClassStubElementType(
    "MACRO_INTERFACE_DECLARATION", (stub, type) -> new HaxeMacroInterfaceDeclarationImpl(stub, type));
  HaxeClassStubElementType MACRO_ENUM_DECLARATION = new HaxeClassStubElementType(
    "MACRO_ENUM_DECLARATION", (stub, type) -> new HaxeMacroEnumDeclarationImpl(stub, type));
  HaxeClassStubElementType MACRO_ABSTRACT_TYPE_DECLARATION = new HaxeClassStubElementType(
    "MACRO_ABSTRACT_TYPE_DECLARATION", (stub, type) -> new HaxeMacroAbstractTypeDeclarationImpl(stub, type));
  HaxeClassStubElementType MACRO_TYPEDEF_DECLARATION = new HaxeClassStubElementType(
    "MACRO_TYPEDEF_DECLARATION", (stub, type) -> new HaxeMacroTypedefDeclarationImpl(stub, type));
  HaxeClassStubElementType MACRO_EXTERN_CLASS_DECLARATION = new HaxeClassStubElementType(
    "MACRO_EXTERN_CLASS_DECLARATION", (stub, type) -> new HaxeMacroExternClassDeclarationImpl(stub, type));
  HaxeClassStubElementType MACRO_EXTERN_INTERFACE_DECLARATION = new HaxeClassStubElementType(
    "MACRO_EXTERN_INTERFACE_DECLARATION", (stub, type) -> new HaxeMacroExternInterfaceDeclarationImpl(stub, type));

  // --- Member declarations ---
  HaxeMethodStubElementType METHOD_DECLARATION = new HaxeMethodStubElementType(
    "METHOD_DECLARATION", (stub, type) -> new HaxeMethodDeclarationImpl(stub, type));
  HaxeMethodStubElementType CONSTRUCTOR_DECLARATION = new HaxeMethodStubElementType(
    "CONSTRUCTOR_DECLARATION", (stub, type) -> new HaxeConstructorDeclarationImpl(stub, type));
  HaxeMethodStubElementType ENUM_VALUE_DECLARATION_CONSTRUCTOR = new HaxeMethodStubElementType(
    "ENUM_VALUE_DECLARATION_CONSTRUCTOR", (stub, type) -> new HaxeEnumValueDeclarationConstructorImpl(stub, type));
  HaxeMethodStubElementType MODULE_METHOD_DECLARATION = new HaxeMethodStubElementType(
    "MODULE_METHOD_DECLARATION", (stub, type) -> new HaxeModuleMethodDeclarationImpl(stub, type));

  HaxeFieldStubElementType FIELD_DECLARATION = new HaxeFieldStubElementType(
    "FIELD_DECLARATION", (stub, type) -> new HaxeFieldDeclarationImpl(stub, type));
  HaxeFieldStubElementType ENUM_VALUE_DECLARATION_FIELD = new HaxeFieldStubElementType(
    "ENUM_VALUE_DECLARATION_FIELD", (stub, type) -> new HaxeEnumValueDeclarationFieldImpl(stub, type));
  HaxeFieldStubElementType OPTIONAL_FIELD_DECLARATION = new HaxeFieldStubElementType(
    "OPTIONAL_FIELD_DECLARATION", (stub, type) -> new HaxeOptionalFieldDeclarationImpl(stub, type));
  HaxeFieldStubElementType MODULE_FIELD_DECLARATION = new HaxeFieldStubElementType(
    "MODULE_FIELD_DECLARATION", (stub, type) -> new HaxeModuleFieldDeclarationImpl(stub, type));

  // ---File Structure and imports ---
  HaxePackageStubElementType PACKAGE_STATEMENT = new HaxePackageStubElementType();
  HaxeImportStubElementType IMPORT_STATEMENT = new HaxeImportStubElementType();
  HaxeUsingStubElementType USING_STATEMENT = new HaxeUsingStubElementType();
  HaxeModuleStubElementType MODULE = new HaxeModuleStubElementType();

}


