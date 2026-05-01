package com.intellij.plugins.haxe.model;

import org.intellij.lang.annotations.MagicConstant;

public class HaxeCompilerMetadata {
    public static final String ABI = "@:abi";
    public static final String ABSTRACT = "@:abstract";
    public static final String ACCESS = "@:access";
    public static final String ALLOW = "@:allow";
    public static final String ANALYZER = "@:analyzer";
    public static final String ANNOTATION = "@:annotation";
    public static final String ARRAY_ACCESS = "@:arrayAccess";
    public static final String CS_ASSEMBLY_META = "@:cs.assemblyMeta";
    public static final String CS_ASSEMBLY_STRICT = "@:cs.assemblyStrict";
    public static final String AST_SOURCE = "@:astSource";
    public static final String AUTO_BUILD = "@:autoBuild";
    public static final String BIND = "@:bind";
    public static final String BITMAP = "@:bitmap";
    public static final String BRIDGE_PROPERTIES = "@:bridgeProperties";
    public static final String BUILD = "@:build";
    public static final String BUILD_XML = "@:buildXml";
    public static final String BYPASS_ACCESSOR = "@:bypassAccessor";
    public static final String CALLABLE = "@:callable";
    public static final String CLASS_CODE = "@:classCode";
    public static final String COMMUTATIVE = "@:commutative";
    public static final String CONST = "@:const";
    public static final String CORE_API = "@:coreApi";
    public static final String CORE_TYPE = "@:coreType";
    public static final String CPP_FILE_CODE = "@:cppFileCode";
    public static final String CPP_INCLUDE = "@:cppInclude";
    public static final String CPP_NAMESPACE_CODE = "@:cppNamespaceCode";
    public static final String CS_USING = "@:cs.using";
    public static final String DCE = "@:dce";
    public static final String DEBUG = "@:debug";
    public static final String DECL = "@:decl";
    public static final String DELEGATE = "@:delegate";
    public static final String DEPEND = "@:depend";
    public static final String DEPRECATED = "@:deprecated";
    public static final String EAGER = "@:eager";
    public static final String ENUM = "@:enum";
    public static final String EVENT = "@:event";
    public static final String EXPOSE = "@:expose";
    public static final String EXTERN = "@:extern";
    public static final String FILE = "@:file";
    public static final String FILE_XML = "@:fileXml";
    public static final String FINAL = "@:final";
    public static final String FIXED = "@:fixed";
    public static final String FLASH_PROPERTY = "@:flash.property";
    public static final String FONT = "@:font";
    public static final String FORWARD = "@:forward";
    public static final String FORWARD_NEW = "@:forward.new";
    public static final String FORWARD_STATICS = "@:forwardStatics";
    public static final String FORWARD_VARIANCE = "@:forward.variance";
    public static final String FROM = "@:from";
    public static final String FUNCTION_CODE = "@:functionCode";
    public static final String FUNCTION_TAIL_CODE = "@:functionTailCode";
    public static final String GENERIC = "@:generic";
    public static final String GENERIC_BUILD = "@:genericBuild";
    public static final String GENERIC_CLASS_PER_METHOD = "@:genericClassPerMethod";
    public static final String GETTER = "@:getter";
    public static final String HACK = "@:hack";
    public static final String HEADER_CLASS_CODE = "@:headerClassCode";
    public static final String HEADER_CODE = "@:headerCode";
    public static final String HEADER_INCLUDE = "@:headerInclude";
    public static final String HEADER_NAMESPACE_CODE = "@:headerNamespaceCode";
    public static final String HL_NATIVE = "@:hlNative";
    public static final String HX_GEN = "@:hxGen";
    public static final String IF_FEATURE = "@:ifFeature";
    public static final String PYTHON_IMPORT = "@:pythonImport";
    public static final String INCLUDE = "@:include";
    public static final String INHERIT_DOC = "@:inheritDoc";
    public static final String INLINE = "@:inline";
    public static final String INTERNAL = "@:internal";
    public static final String IS_VAR = "@:isVar";
    public static final String JAVA_CANONICAL = "@:javaCanonical";
    public static final String JAVA_DEFAULT = "@:java.default";
    public static final String JVM_SYNTHETIC = "@:jvm.synthetic";
    public static final String JS_REQUIRE = "@:jsRequire";
    public static final String LUA_REQUIRE = "@:luaRequire";
    public static final String LUA_DOT_METHOD = "@:luaDotMethod";
    public static final String KEEP = "@:keep";
    public static final String KEEP_INIT = "@:keepInit";
    public static final String KEEP_SUB = "@:keepSub";
    public static final String MARKUP = "@:markup";
    public static final String MACRO = "@:macro";
    public static final String MERGE_BLOCK = "@:mergeBlock";
    public static final String MULTI_RETURN = "@:multiReturn";
    public static final String MULTI_TYPE = "@:multiType";
    public static final String NATIVE = "@:native";
    public static final String JAVA_NATIVE = "@:java.native";
    public static final String NATIVE_CHILDREN = "@:nativeChildren";
    public static final String NATIVE_GEN = "@:nativeGen";
    public static final String NATIVE_PROPERTY = "@:nativeProperty";
    public static final String NATIVE_STATIC_EXTENSION = "@:nativeStaticExtension";
    public static final String NO_COMPLETION = "@:noCompletion";
    public static final String NO_CLOSURE = "@:noClosure";
    public static final String NO_DEBUG = "@:noDebug";
    public static final String NO_DOC = "@:noDoc";
    public static final String NO_IMPORT_GLOBAL = "@:noImportGlobal";
    public static final String NON_VIRTUAL = "@:nonVirtual";
    public static final String NO_PRIVATE_ACCESS = "@:noPrivateAccess";
    public static final String NO_STACK = "@:noStack";
    public static final String NOT_NULL = "@:notNull";
    public static final String NO_USING = "@:noUsing";
    public static final String NULL_SAFETY = "@:nullSafety";
    public static final String OBJC = "@:objc";
    public static final String OBJC_PROTOCOL = "@:objcProtocol";
    public static final String OP = "@:op";
    public static final String OPTIONAL = "@:optional";
    public static final String OVERLOAD = "@:overload";
    public static final String PERSISTENT = "@:persistent";
    public static final String PHP_ATTRIBUTE = "@:php.attribute";
    public static final String PHP_GLOBAL = "@:phpGlobal";
    public static final String PHP_CLASS_CONST = "@:phpClassConst";
    public static final String PHP_MAGIC = "@:phpMagic";
    public static final String PHP_NO_CONSTRUCTOR = "@:phpNoConstructor";
    public static final String POS = "@:pos";
    public static final String PUBLIC_FIELDS = "@:publicFields";
    public static final String PRIVATE = "@:private";
    public static final String PRIVATE_ACCESS = "@:privateAccess";
    public static final String PROTECTED = "@:protected";
    public static final String PROPERTY = "@:property";
    public static final String PURE = "@:pure";
    public static final String READ_ONLY = "@:readOnly";
    public static final String REMOVE = "@:remove";
    public static final String REQUIRE = "@:require";
    public static final String RESOLVE = "@:resolve";
    public static final String RTTI = "@:rtti";
    public static final String RUNTIME_VALUE = "@:runtimeValue";
    public static final String SCALAR = "@:scalar";
    public static final String SELF_CALL = "@:selfCall";
    public static final String SEMANTICS = "@:semantics";
    public static final String SETTER = "@:setter";
    public static final String SOUND = "@:sound";
    public static final String SOURCE_FILE = "@:sourceFile";
    public static final String STACK_ONLY = "@:stackOnly";
    public static final String STRICT = "@:strict";
    public static final String STRUCT = "@:struct";
    public static final String STRUCT_ACCESS = "@:structAccess";
    public static final String STRUCT_INIT = "@:structInit";
    public static final String SUPPRESS_WARNINGS = "@:suppressWarnings";
    public static final String TEMPLATED_CALL = "@:templatedCall";
    public static final String THROWS = "@:throws";
    public static final String TO = "@:to";
    public static final String TRANSIENT = "@:transient";
    public static final String TRANSITIVE = "@:transitive";
    public static final String VOLATILE = "@:volatile";
    public static final String UNIFY_MIN_DYNAMIC = "@:unifyMinDynamic";
    public static final String UNREFLECTIVE = "@:unreflective";
    public static final String UNSAFE = "@:unsafe";
    public static final String USING = "@:using";
    public static final String HAXE_WARNING = "@:haxe.warning";
    public static final String VOID = "@:void";
    public static final String NATIVE_ARRAY_ACCESS = "@:nativeArrayAccess";

    @MagicConstant(stringValues = {
            ABI, ABSTRACT, ACCESS, ALLOW, ANALYZER, ANNOTATION, ARRAY_ACCESS, CS_ASSEMBLY_META, CS_ASSEMBLY_STRICT,
            AST_SOURCE, AUTO_BUILD, BIND, BITMAP, BRIDGE_PROPERTIES, BUILD, BUILD_XML, BYPASS_ACCESSOR, CALLABLE,
            CLASS_CODE, COMMUTATIVE, CONST, CORE_API, CORE_TYPE, CPP_FILE_CODE, CPP_INCLUDE, CPP_NAMESPACE_CODE,
            CS_USING, DCE, DEBUG, DECL, DELEGATE, DEPEND, DEPRECATED, EAGER, ENUM, EVENT, EXPOSE, EXTERN, FILE,
            FILE_XML, FINAL, FIXED, FLASH_PROPERTY, FONT, FORWARD, FORWARD_NEW, FORWARD_STATICS, FORWARD_VARIANCE,
            FROM, FUNCTION_CODE, FUNCTION_TAIL_CODE, GENERIC, GENERIC_BUILD, GENERIC_CLASS_PER_METHOD, GETTER, HACK,
            HEADER_CLASS_CODE, HEADER_CODE, HEADER_INCLUDE, HEADER_NAMESPACE_CODE, HL_NATIVE, HX_GEN, IF_FEATURE,
            PYTHON_IMPORT, INCLUDE, INHERIT_DOC, INLINE, INTERNAL, IS_VAR, JAVA_CANONICAL, JAVA_DEFAULT, JVM_SYNTHETIC,
            JS_REQUIRE, LUA_REQUIRE, LUA_DOT_METHOD, KEEP, KEEP_INIT, KEEP_SUB, MARKUP, MACRO, MERGE_BLOCK,
            MULTI_RETURN, MULTI_TYPE, NATIVE, JAVA_NATIVE, NATIVE_CHILDREN, NATIVE_GEN, NATIVE_PROPERTY,
            NATIVE_STATIC_EXTENSION, NO_COMPLETION, NO_CLOSURE, NO_DEBUG, NO_DOC, NO_IMPORT_GLOBAL, NON_VIRTUAL,
            NO_PRIVATE_ACCESS, NO_STACK, NOT_NULL, NO_USING, NULL_SAFETY, OBJC, OBJC_PROTOCOL, OP, OPTIONAL, OVERLOAD,
            PERSISTENT, PHP_ATTRIBUTE, PHP_GLOBAL, PHP_CLASS_CONST, PHP_MAGIC, PHP_NO_CONSTRUCTOR, POS, PUBLIC_FIELDS,
            PRIVATE, PRIVATE_ACCESS, PROTECTED, PROPERTY, PURE, READ_ONLY, REMOVE, REQUIRE, RESOLVE, RTTI,
            RUNTIME_VALUE, SCALAR, SELF_CALL, SEMANTICS, SETTER, SOUND, SOURCE_FILE, STACK_ONLY, STRICT, STRUCT,
            STRUCT_ACCESS, STRUCT_INIT, SUPPRESS_WARNINGS, TEMPLATED_CALL, THROWS, TO, TRANSIENT, TRANSITIVE, VOLATILE,
            UNIFY_MIN_DYNAMIC, UNREFLECTIVE, UNSAFE, USING, HAXE_WARNING, VOID, NATIVE_ARRAY_ACCESS
    })
    public @interface CompilerMetadata {

    }
}
