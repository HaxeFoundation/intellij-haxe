package com.intellij.plugins.haxe.lang.psi.stubs;

import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.lang.psi.stubs.factories.*;
import com.intellij.plugins.haxe.lang.psi.stubs.serializers.*;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.psi.stubs.StubRegistry;
import com.intellij.psi.stubs.StubRegistryExtension;
import org.jspecify.annotations.NonNull;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;


public class HaxeStubRegistryExtension implements StubRegistryExtension {
    @Override
    public void register(@NonNull StubRegistry stubRegistry) {

        //TODO clean up, no need for element type here
        stubRegistry.registerStubSerializer(HaxeTokenTypeSets.HAXE_FILE, new HaxeFileStubSerializer());

        stubRegistry.registerStubSerializer(MODULE, new HaxeModuleStubSerializer(MODULE));
        stubRegistry.registerStubFactory(MODULE, new HaxeModuleStubFactory((HaxeElementType)MODULE));

        registerHaxeClassTypes(stubRegistry);
        registerHaxeMethodTypes(stubRegistry);
        registerHaxeFieldTypes(stubRegistry);
        registerHaxeImportTypes(stubRegistry);


//
        stubRegistry.registerStubSerializer(COMPONENT_NAME, new HaxeComponentNameStubSerializer(COMPONENT_NAME));
        stubRegistry.registerStubFactory(COMPONENT_NAME, new HaxeComponentNameStubFactory((HaxeElementType)COMPONENT_NAME, HaxeComponentNameImpl::new));

        stubRegistry.registerStubSerializer(REFERENCE_EXPRESSION, new HaxeReferenceExpressionStubSerializer(REFERENCE_EXPRESSION));
        stubRegistry.registerStubFactory(REFERENCE_EXPRESSION, new HaxeReferenceExpressionStubFactory((HaxeElementType)REFERENCE_EXPRESSION, HaxeReferenceExpressionImpl::new));

        stubRegistry.registerStubSerializer(TYPE_TAG, new HaxeContainerStubSerializer<>(TYPE_TAG));
        stubRegistry.registerStubFactory(TYPE_TAG, new HaxeContainerStubFactory<>((HaxeElementType)TYPE_TAG, HaxeTypeTagImpl::new));
        stubRegistry.registerStubSerializer(TYPE_OR_ANONYMOUS, new HaxeContainerStubSerializer<>(TYPE_OR_ANONYMOUS));
        stubRegistry.registerStubFactory(TYPE_OR_ANONYMOUS, new HaxeContainerStubFactory<>((HaxeElementType)TYPE_OR_ANONYMOUS, HaxeTypeOrAnonymousImpl::new));
        stubRegistry.registerStubSerializer(TYPE, new HaxeContainerStubSerializer<>(TYPE));
        stubRegistry.registerStubFactory(TYPE, new HaxeContainerStubFactory<>((HaxeElementType)TYPE, HaxeTypeImpl::new));
        stubRegistry.registerStubSerializer(TYPE_PARAM, new HaxeContainerStubSerializer<>(TYPE_PARAM));
        stubRegistry.registerStubFactory(TYPE_PARAM, new HaxeContainerStubFactory<>((HaxeElementType)TYPE_PARAM, HaxeTypeParamImpl::new));
        stubRegistry.registerStubSerializer(TYPE_LIST_PART, new HaxeContainerStubSerializer<>(TYPE_LIST_PART));
        stubRegistry.registerStubFactory(TYPE_LIST_PART, new HaxeContainerStubFactory<>((HaxeElementType)TYPE_LIST_PART, HaxeTypeListPartImpl::new));
        stubRegistry.registerStubSerializer(FUNCTION_TYPE, new HaxeContainerStubSerializer<>(FUNCTION_TYPE));
        stubRegistry.registerStubFactory(FUNCTION_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)FUNCTION_TYPE, HaxeFunctionTypeImpl::new));
        stubRegistry.registerStubSerializer(FUNCTION_ARGUMENT, new HaxeContainerStubSerializer<>(FUNCTION_ARGUMENT));
        stubRegistry.registerStubFactory(FUNCTION_ARGUMENT, new HaxeContainerStubFactory<>((HaxeElementType)FUNCTION_ARGUMENT, HaxeFunctionArgumentImpl::new));
        stubRegistry.registerStubSerializer(FUNCTION_RETURN_TYPE, new HaxeContainerStubSerializer<>(FUNCTION_RETURN_TYPE));
        stubRegistry.registerStubFactory(FUNCTION_RETURN_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)FUNCTION_RETURN_TYPE, HaxeFunctionReturnTypeImpl::new));
        stubRegistry.registerStubSerializer(REST_ARGUMENT_TYPE, new HaxeContainerStubSerializer<>(REST_ARGUMENT_TYPE));
        stubRegistry.registerStubFactory(REST_ARGUMENT_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)REST_ARGUMENT_TYPE, HaxeRestArgumentTypeImpl::new));
        stubRegistry.registerStubSerializer(OLD_REST_ARGUMENT_TYPE, new HaxeContainerStubSerializer<>(OLD_REST_ARGUMENT_TYPE));
        stubRegistry.registerStubFactory(OLD_REST_ARGUMENT_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)OLD_REST_ARGUMENT_TYPE, HaxeOldRestArgumentTypeImpl::new));

        stubRegistry.registerStubSerializer(PARAMETER, new HaxeParameterStubSerializer(PARAMETER));
        stubRegistry.registerStubFactory(PARAMETER, new HaxeParameterStubFactory((HaxeElementType)PARAMETER, HaxeParameterImpl::new));
        stubRegistry.registerStubSerializer(REST_PARAMETER, new HaxeParameterStubSerializer(REST_PARAMETER));
        stubRegistry.registerStubFactory(REST_PARAMETER, new HaxeParameterStubFactory((HaxeElementType)REST_PARAMETER, HaxeRestParameterImpl::new));
        stubRegistry.registerStubSerializer(UNTYPED_PARAMETER, new HaxeParameterStubSerializer(UNTYPED_PARAMETER));
        stubRegistry.registerStubFactory(UNTYPED_PARAMETER, new HaxeParameterStubFactory((HaxeElementType)UNTYPED_PARAMETER, HaxeUntypedParameterImpl::new));

        stubRegistry.registerStubSerializer(GENERIC_LIST_PART, new HaxeGenericListPartStubSerializer(GENERIC_LIST_PART));
        stubRegistry.registerStubFactory(GENERIC_LIST_PART, new HaxeGenericListPartStubFactory((HaxeElementType)GENERIC_LIST_PART, HaxeGenericListPartImpl::new));
        stubRegistry.registerStubSerializer(GENERIC_PARAM, new HaxeContainerStubSerializer<>(GENERIC_PARAM));
        stubRegistry.registerStubFactory(GENERIC_PARAM, new HaxeContainerStubFactory<>((HaxeElementType)GENERIC_PARAM, HaxeGenericParamImpl::new));
        stubRegistry.registerStubSerializer(GENERIC_DEFAULT_TYPE, new HaxeContainerStubSerializer<>(GENERIC_DEFAULT_TYPE));
        stubRegistry.registerStubFactory(GENERIC_DEFAULT_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)GENERIC_DEFAULT_TYPE, HaxeGenericDefaultTypeImpl::new));
        stubRegistry.registerStubSerializer(GENERIC_CONSTRAINT_PART, new HaxeContainerStubSerializer<>(GENERIC_CONSTRAINT_PART));
        stubRegistry.registerStubFactory(GENERIC_CONSTRAINT_PART, new HaxeContainerStubFactory<>((HaxeElementType)GENERIC_CONSTRAINT_PART, HaxeGenericConstraintPartImpl::new));

        stubRegistry.registerStubSerializer(PARAMETER_LIST, new HaxeContainerStubSerializer<>(PARAMETER_LIST));
        stubRegistry.registerStubFactory(PARAMETER_LIST, new HaxeContainerStubFactory<>((HaxeElementType)PARAMETER_LIST, HaxeParameterListImpl::new));
        stubRegistry.registerStubSerializer(OPEN_PARAMETER_LIST, new HaxeContainerStubSerializer<>(OPEN_PARAMETER_LIST));
        stubRegistry.registerStubFactory(OPEN_PARAMETER_LIST, new HaxeContainerStubFactory<>((HaxeElementType)OPEN_PARAMETER_LIST, HaxeOpenParameterListImpl::new));

        stubRegistry.registerStubSerializer(ANONYMOUS_TYPE_BODY, new HaxeContainerStubSerializer<>(ANONYMOUS_TYPE_BODY));
        stubRegistry.registerStubFactory(ANONYMOUS_TYPE_BODY, new HaxeContainerStubFactory<>((HaxeElementType)ANONYMOUS_TYPE_BODY, HaxeAnonymousTypeBodyImpl::new));
        stubRegistry.registerStubSerializer(TYPE_EXTENDS_LIST, new HaxeContainerStubSerializer<>(TYPE_EXTENDS_LIST));
        stubRegistry.registerStubFactory(TYPE_EXTENDS_LIST, new HaxeContainerStubFactory<>((HaxeElementType)TYPE_EXTENDS_LIST, HaxeTypeExtendsListImpl::new));
        stubRegistry.registerStubSerializer(ANONYMOUS_TYPE_FIELD_LIST, new HaxeContainerStubSerializer<>(ANONYMOUS_TYPE_FIELD_LIST));
        stubRegistry.registerStubFactory(ANONYMOUS_TYPE_FIELD_LIST, new HaxeContainerStubFactory<>((HaxeElementType)ANONYMOUS_TYPE_FIELD_LIST, HaxeAnonymousTypeFieldListImpl::new));

        stubRegistry.registerStubSerializer(INHERIT_LIST, new HaxeContainerStubSerializer<>(INHERIT_LIST));
        stubRegistry.registerStubFactory(INHERIT_LIST, new HaxeContainerStubFactory<>((HaxeElementType)INHERIT_LIST, HaxeInheritListImpl::new));
        stubRegistry.registerStubSerializer(EXTENDS_DECLARATION, new HaxeContainerStubSerializer<>(EXTENDS_DECLARATION));
        stubRegistry.registerStubFactory(EXTENDS_DECLARATION, new HaxeContainerStubFactory<>((HaxeElementType)EXTENDS_DECLARATION, HaxeExtendsDeclarationImpl::new));
        stubRegistry.registerStubSerializer(IMPLEMENTS_DECLARATION, new HaxeContainerStubSerializer<>(IMPLEMENTS_DECLARATION));
        stubRegistry.registerStubFactory(IMPLEMENTS_DECLARATION, new HaxeContainerStubFactory<>((HaxeElementType)IMPLEMENTS_DECLARATION, HaxeImplementsDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ABSTRACT_TO_TYPE, new HaxeContainerStubSerializer<>(ABSTRACT_TO_TYPE));
        stubRegistry.registerStubFactory(ABSTRACT_TO_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)ABSTRACT_TO_TYPE, HaxeAbstractToTypeImpl::new));
        stubRegistry.registerStubSerializer(ABSTRACT_FROM_TYPE, new HaxeContainerStubSerializer<>(ABSTRACT_FROM_TYPE));
        stubRegistry.registerStubFactory(ABSTRACT_FROM_TYPE, new HaxeContainerStubFactory<>((HaxeElementType)ABSTRACT_FROM_TYPE, HaxeAbstractFromTypeImpl::new));
    }

    private static void registerHaxeImportTypes(@NonNull StubRegistry stubRegistry) {
        stubRegistry.registerStubSerializer(PACKAGE_STATEMENT, new HaxePackageStubSerializer(PACKAGE_STATEMENT));
        stubRegistry.registerStubFactory(PACKAGE_STATEMENT, new HaxePackageStubFactory((HaxeElementType)PACKAGE_STATEMENT));
        stubRegistry.registerStubSerializer(IMPORT_STATEMENT, new HaxeImportStubSerializer(IMPORT_STATEMENT));
        stubRegistry.registerStubFactory(IMPORT_STATEMENT, new HaxeImportStubFactory((HaxeElementType)IMPORT_STATEMENT));
        stubRegistry.registerStubSerializer(USING_STATEMENT, new HaxeUsingStubSerializer(USING_STATEMENT));
        stubRegistry.registerStubFactory(USING_STATEMENT, new HaxeUsingStubFactory((HaxeElementType)USING_STATEMENT));
    }

    private static void registerHaxeFieldTypes(@NonNull StubRegistry stubRegistry) {
        stubRegistry.registerStubSerializer(FIELD_DECLARATION, new HaxeFieldStubSerializer(FIELD_DECLARATION));
        stubRegistry.registerStubFactory(FIELD_DECLARATION, new HaxeFieldStubFactory((HaxeElementType)FIELD_DECLARATION, HaxeFieldDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MODULE_FIELD_DECLARATION, new HaxeFieldStubSerializer(MODULE_FIELD_DECLARATION));
        stubRegistry.registerStubFactory(MODULE_FIELD_DECLARATION, new HaxeFieldStubFactory((HaxeElementType)MODULE_FIELD_DECLARATION, HaxeModuleFieldDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ENUM_VALUE_DECLARATION_FIELD, new HaxeFieldStubSerializer(ENUM_VALUE_DECLARATION_FIELD));
        stubRegistry.registerStubFactory(ENUM_VALUE_DECLARATION_FIELD, new HaxeFieldStubFactory((HaxeElementType)ENUM_VALUE_DECLARATION_FIELD, HaxeEnumValueDeclarationFieldImpl::new));

        stubRegistry.registerStubSerializer(OPTIONAL_FIELD_DECLARATION, new HaxeFieldStubSerializer(OPTIONAL_FIELD_DECLARATION));
        stubRegistry.registerStubFactory(OPTIONAL_FIELD_DECLARATION, new HaxeFieldStubFactory((HaxeElementType)OPTIONAL_FIELD_DECLARATION, HaxeOptionalFieldDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ANONYMOUS_TYPE_FIELD, new HaxeFieldStubSerializer(ANONYMOUS_TYPE_FIELD));
        stubRegistry.registerStubFactory(ANONYMOUS_TYPE_FIELD, new HaxeFieldStubFactory((HaxeElementType)ANONYMOUS_TYPE_FIELD, HaxeAnonymousTypeFieldImpl::new));
    }

    private static void registerHaxeMethodTypes(@NonNull StubRegistry stubRegistry) {
        stubRegistry.registerStubSerializer(METHOD_DECLARATION, new HaxeMethodStubSerializer(METHOD_DECLARATION));
        stubRegistry.registerStubFactory(METHOD_DECLARATION, new HaxeMethodStubFactory((HaxeElementType)METHOD_DECLARATION, HaxeMethodDeclarationImpl::new));

        stubRegistry.registerStubSerializer(CONSTRUCTOR_DECLARATION, new HaxeMethodStubSerializer(CONSTRUCTOR_DECLARATION));
        stubRegistry.registerStubFactory(CONSTRUCTOR_DECLARATION, new HaxeMethodStubFactory((HaxeElementType)CONSTRUCTOR_DECLARATION, HaxeConstructorDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ENUM_VALUE_DECLARATION_CONSTRUCTOR, new HaxeMethodStubSerializer(ENUM_VALUE_DECLARATION_CONSTRUCTOR));
        stubRegistry.registerStubFactory(ENUM_VALUE_DECLARATION_CONSTRUCTOR, new HaxeMethodStubFactory((HaxeElementType)ENUM_VALUE_DECLARATION_CONSTRUCTOR, HaxeEnumValueDeclarationConstructorImpl::new));

        stubRegistry.registerStubSerializer(MODULE_METHOD_DECLARATION, new HaxeMethodStubSerializer(MODULE_METHOD_DECLARATION));
        stubRegistry.registerStubFactory(MODULE_METHOD_DECLARATION, new HaxeMethodStubFactory((HaxeElementType)MODULE_METHOD_DECLARATION, HaxeModuleMethodDeclarationImpl::new));
    }

    private static void registerHaxeClassTypes(@NonNull StubRegistry stubRegistry) {
        stubRegistry.registerStubSerializer(CLASS_DECLARATION, new HaxeClassStubSerializer(CLASS_DECLARATION));
        stubRegistry.registerStubFactory(CLASS_DECLARATION, new HaxeClassStubFactory((HaxeElementType)CLASS_DECLARATION, HaxeClassDeclarationImpl::new));

        stubRegistry.registerStubSerializer(INTERFACE_DECLARATION, new HaxeClassStubSerializer(INTERFACE_DECLARATION));
        stubRegistry.registerStubFactory(INTERFACE_DECLARATION, new HaxeClassStubFactory((HaxeElementType)INTERFACE_DECLARATION, HaxeInterfaceDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ENUM_DECLARATION, new HaxeClassStubSerializer(ENUM_DECLARATION));
        stubRegistry.registerStubFactory(ENUM_DECLARATION, new HaxeClassStubFactory((HaxeElementType)ENUM_DECLARATION, HaxeEnumDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ABSTRACT_TYPE_DECLARATION, new HaxeClassStubSerializer(ABSTRACT_TYPE_DECLARATION));
        stubRegistry.registerStubFactory(ABSTRACT_TYPE_DECLARATION, new HaxeClassStubFactory((HaxeElementType)ABSTRACT_TYPE_DECLARATION, HaxeAbstractTypeDeclarationImpl::new));

        stubRegistry.registerStubSerializer(TYPEDEF_DECLARATION, new HaxeClassStubSerializer(TYPEDEF_DECLARATION));
        stubRegistry.registerStubFactory(TYPEDEF_DECLARATION, new HaxeClassStubFactory((HaxeElementType)TYPEDEF_DECLARATION, HaxeTypedefDeclarationImpl::new));

        stubRegistry.registerStubSerializer(EXTERN_CLASS_DECLARATION, new HaxeClassStubSerializer(EXTERN_CLASS_DECLARATION));
        stubRegistry.registerStubFactory(EXTERN_CLASS_DECLARATION, new HaxeClassStubFactory((HaxeElementType)EXTERN_CLASS_DECLARATION, HaxeExternClassDeclarationImpl::new));

        stubRegistry.registerStubSerializer(EXTERN_INTERFACE_DECLARATION, new HaxeClassStubSerializer(EXTERN_INTERFACE_DECLARATION));
        stubRegistry.registerStubFactory(EXTERN_INTERFACE_DECLARATION, new HaxeClassStubFactory((HaxeElementType)EXTERN_INTERFACE_DECLARATION, HaxeExternInterfaceDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_CLASS_DECLARATION, new HaxeClassStubSerializer(MACRO_CLASS_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_CLASS_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_CLASS_DECLARATION, HaxeMacroClassDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_INTERFACE_DECLARATION, new HaxeClassStubSerializer(MACRO_INTERFACE_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_INTERFACE_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_INTERFACE_DECLARATION, HaxeMacroInterfaceDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_ENUM_DECLARATION, new HaxeClassStubSerializer(MACRO_ENUM_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_ENUM_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_ENUM_DECLARATION, HaxeMacroEnumDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_ABSTRACT_TYPE_DECLARATION, new HaxeClassStubSerializer(MACRO_ABSTRACT_TYPE_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_ABSTRACT_TYPE_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_ABSTRACT_TYPE_DECLARATION, HaxeMacroAbstractTypeDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_TYPEDEF_DECLARATION, new HaxeClassStubSerializer(MACRO_TYPEDEF_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_TYPEDEF_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_TYPEDEF_DECLARATION, HaxeMacroTypedefDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_EXTERN_CLASS_DECLARATION, new HaxeClassStubSerializer(MACRO_EXTERN_CLASS_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_EXTERN_CLASS_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_EXTERN_CLASS_DECLARATION, HaxeMacroExternClassDeclarationImpl::new));

        stubRegistry.registerStubSerializer(MACRO_EXTERN_INTERFACE_DECLARATION, new HaxeClassStubSerializer(MACRO_EXTERN_INTERFACE_DECLARATION));
        stubRegistry.registerStubFactory(MACRO_EXTERN_INTERFACE_DECLARATION, new HaxeClassStubFactory((HaxeElementType)MACRO_EXTERN_INTERFACE_DECLARATION, HaxeMacroExternInterfaceDeclarationImpl::new));

        stubRegistry.registerStubSerializer(ANONYMOUS_TYPE, new HaxeClassStubSerializer(ANONYMOUS_TYPE));
        stubRegistry.registerStubFactory(ANONYMOUS_TYPE, new HaxeClassStubFactory((HaxeElementType)ANONYMOUS_TYPE, HaxeAnonymousTypeImpl::new));
    }
}
