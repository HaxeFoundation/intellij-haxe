package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeComponentNameStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeReferenceExpressionStub;
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

  // --- Component name ---
  HaxeComponentNameElementType COMPONENT_NAME =
    new HaxeComponentNameElementType("COMPONENT_NAME", (stub, type) -> new HaxeComponentNameImpl(stub, type));

  // --- Reference expression (stub for use in type sub-tree) ---
  HaxeReferenceExpressionElementType REFERENCE_EXPRESSION =
    new HaxeReferenceExpressionElementType("REFERENCE_EXPRESSION", (stub, type) -> new HaxeReferenceExpressionImpl(stub, type));

  // --- Type sub-tree (container stubs) ---
  HaxeContainerStubElementType<HaxeTypeTag> TYPE_TAG =
    new HaxeContainerStubElementType<>("TYPE_TAG", (stub, type) -> new HaxeTypeTagImpl(stub, type));
  HaxeContainerStubElementType<HaxeTypeOrAnonymous> TYPE_OR_ANONYMOUS =
    new HaxeContainerStubElementType<>("TYPE_OR_ANONYMOUS", (stub, type) -> new HaxeTypeOrAnonymousImpl(stub, type));
  HaxeContainerStubElementType<HaxeType> TYPE =
    new HaxeContainerStubElementType<>("TYPE", (stub, type) -> new HaxeTypeImpl(stub, type));
  HaxeContainerStubElementType<HaxeTypeParam> TYPE_PARAM =
    new HaxeContainerStubElementType<>("TYPE_PARAM", (stub, type) -> new HaxeTypeParamImpl(stub, type));
  HaxeContainerStubElementType<HaxeTypeListPart> TYPE_LIST_PART =
    new HaxeContainerStubElementType<>("TYPE_LIST_PART", (stub, type) -> new HaxeTypeListPartImpl(stub, type));
  HaxeContainerStubElementType<HaxeFunctionType> FUNCTION_TYPE =
    new HaxeContainerStubElementType<>("FUNCTION_TYPE", (stub, type) -> new HaxeFunctionTypeImpl(stub, type));
  HaxeContainerStubElementType<HaxeFunctionArgument> FUNCTION_ARGUMENT =
    new HaxeContainerStubElementType<>("FUNCTION_ARGUMENT", (stub, type) -> new HaxeFunctionArgumentImpl(stub, type));
  HaxeContainerStubElementType<HaxeFunctionReturnType> FUNCTION_RETURN_TYPE =
    new HaxeContainerStubElementType<>("FUNCTION_RETURN_TYPE", (stub, type) -> new HaxeFunctionReturnTypeImpl(stub, type));
  HaxeContainerStubElementType<HaxeRestArgumentType> REST_ARGUMENT_TYPE =
    new HaxeContainerStubElementType<>("REST_ARGUMENT_TYPE", (stub, type) -> new HaxeRestArgumentTypeImpl(stub, type));
  HaxeContainerStubElementType<HaxeRestArgumentType> OLD_REST_ARGUMENT_TYPE =
    new HaxeContainerStubElementType<>("OLD_REST_ARGUMENT_TYPE", (stub, type) -> new HaxeOldRestArgumentTypeImpl(stub, type));

  // --- Parameters ---
  HaxeParameterStubElementType PARAMETER = new HaxeParameterStubElementType(
    "PARAMETER", (stub, type) -> new HaxeParameterImpl(stub, type));
  HaxeParameterStubElementType REST_PARAMETER = new HaxeParameterStubElementType(
    "REST_PARAMETER", (stub, type) -> new HaxeRestParameterImpl(stub, type));
  HaxeParameterStubElementType UNTYPED_PARAMETER = new HaxeParameterStubElementType(
    "UNTYPED_PARAMETER", (stub, type) -> new HaxeUntypedParameterImpl(stub, type));
  HaxeContainerStubElementType<HaxeParameterList> PARAMETER_LIST =
    new HaxeContainerStubElementType<>("PARAMETER_LIST", (stub, type) -> new HaxeParameterListImpl(stub, type));
  HaxeContainerStubElementType<HaxeOpenParameterList> OPEN_PARAMETER_LIST =
    new HaxeContainerStubElementType<>("OPEN_PARAMETER_LIST", (stub, type) -> new HaxeOpenParameterListImpl(stub, type));

  // --- Generic type params ---
  HaxeGenericListPartStubElementType GENERIC_LIST_PART =
    new HaxeGenericListPartStubElementType((stub, type) -> new HaxeGenericListPartImpl(stub, type));
  HaxeContainerStubElementType<HaxeGenericParam> GENERIC_PARAM =
    new HaxeContainerStubElementType<>("GENERIC_PARAM", (stub, type) -> new HaxeGenericParamImpl(stub, type));
  HaxeContainerStubElementType<HaxeGenericDefaultType> GENERIC_DEFAULT_TYPE =
    new HaxeContainerStubElementType<>("GENERIC_DEFAULT_TYPE", (stub, type) -> new HaxeGenericDefaultTypeImpl(stub, type));
  HaxeContainerStubElementType<HaxeGenericConstraintPart> GENERIC_CONSTRAINT_PART =
    new HaxeContainerStubElementType<>("GENERIC_CONSTRAINT_PART", (stub, type) -> new HaxeGenericConstraintPartImpl(stub, type));

  // --- Anonymous type (uses HaxeClassStub like other class-like declarations) ---
  HaxeClassStubElementType ANONYMOUS_TYPE = new HaxeClassStubElementType(
    "ANONYMOUS_TYPE", (stub, type) -> new HaxeAnonymousTypeImpl(stub, type));

  // --- Anonymous type children ---
  HaxeContainerStubElementType<HaxeAnonymousTypeBody> ANONYMOUS_TYPE_BODY =
    new HaxeContainerStubElementType<>("ANONYMOUS_TYPE_BODY", (stub, type) -> new HaxeAnonymousTypeBodyImpl(stub, type));
  HaxeContainerStubElementType<HaxeTypeExtendsList> TYPE_EXTENDS_LIST =
    new HaxeContainerStubElementType<>("TYPE_EXTENDS_LIST", (stub, type) -> new HaxeTypeExtendsListImpl(stub, type));
  HaxeContainerStubElementType<HaxeAnonymousTypeFieldList> ANONYMOUS_TYPE_FIELD_LIST =
    new HaxeContainerStubElementType<>("ANONYMOUS_TYPE_FIELD_LIST", (stub, type) -> new HaxeAnonymousTypeFieldListImpl(stub, type));
  HaxeFieldStubElementType ANONYMOUS_TYPE_FIELD = new HaxeFieldStubElementType(
    "ANONYMOUS_TYPE_FIELD", (stub, type) -> new HaxeAnonymousTypeFieldImpl(stub, type));


  // --- Inheritance ---
  HaxeContainerStubElementType<HaxeInheritList> INHERIT_LIST =
    new HaxeContainerStubElementType<>("INHERIT_LIST", (stub, type) -> new HaxeInheritListImpl(stub, type));
  HaxeContainerStubElementType<HaxeExtendsDeclaration> EXTENDS_DECLARATION =
    new HaxeContainerStubElementType<>("EXTENDS_DECLARATION", (stub, type) -> new HaxeExtendsDeclarationImpl(stub, type));
  HaxeContainerStubElementType<HaxeImplementsDeclaration> IMPLEMENTS_DECLARATION =
    new HaxeContainerStubElementType<>("IMPLEMENTS_DECLARATION", (stub, type) -> new HaxeImplementsDeclarationImpl(stub, type));

  // --- abstract to/from ---
  HaxeContainerStubElementType<HaxeAbstractToType> ABSTRACT_TO_TYPE =
    new HaxeContainerStubElementType<>("ABSTRACT_TO_TYPE", (stub, type) -> new HaxeAbstractToTypeImpl(stub, type));
  HaxeContainerStubElementType<HaxeAbstractFromType> ABSTRACT_FROM_TYPE =
    new HaxeContainerStubElementType<>("ABSTRACT_FROM_TYPE", (stub, type) -> new HaxeAbstractFromTypeImpl(stub, type));



}


