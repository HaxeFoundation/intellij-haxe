/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.metadata.psi;

import com.intellij.plugins.haxe.lang.psi.HaxePsiCompositeElement;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HaxeMeta extends HaxePsiCompositeElement {

  /**
   * @return the type element (e.g. "isVar", "final") from the metadata.
   */
  @NotNull
  HaxeMetadataType getType();

  /**
   * @return the content element of the metadata, if any.  Children of this
   * element are Haxe PSI tokens.
   */
  @Nullable
  HaxeMetadataContent getContent();

  /**
   * @return the EMBEDDED_META wrapper for this metadata.
   */
  @NotNull
  PsiElement getContainer();

  boolean isRunTimeMeta();
  boolean isCompileTimeMeta();

  boolean isType(HaxeMetadataTypeName type);
  boolean isType(HaxeMeta meta);
  boolean isType(String name);

  // Metadata classes.
  Class COMPILE_TIME = HaxeMetadataCompileTimeMeta.class;
  Class RUN_TIME = HaxeMetadataRunTimeMeta.class;

  // Metadata types.

  HaxeMetadataTypeName ACCESS = new HaxeMetadataTypeName("access"); // <Target path>
  HaxeMetadataTypeName ALLOW = new HaxeMetadataTypeName("allow"); // <Target path>
  HaxeMetadataTypeName ARRAY_ACCESS = new HaxeMetadataTypeName("arrayAccess");
  HaxeMetadataTypeName AUTO_BUILD = new HaxeMetadataTypeName("autoBuild"); // <Build macro call>
  HaxeMetadataTypeName BIND = new HaxeMetadataTypeName("bind");
  HaxeMetadataTypeName BITMAP = new HaxeMetadataTypeName("bitmap"); // <Bitmap file path>
  HaxeMetadataTypeName BUILD = new HaxeMetadataTypeName("build"); // <Build macro call>
  HaxeMetadataTypeName COMMUTATIVE = new HaxeMetadataTypeName("commutative");
  HaxeMetadataTypeName CONST = new HaxeMetadataTypeName("const");
  HaxeMetadataTypeName CORE_API = new HaxeMetadataTypeName("coreApi");
  HaxeMetadataTypeName CORE_TYPE = new HaxeMetadataTypeName("coreType");
  HaxeMetadataTypeName DEBUG = new HaxeMetadataTypeName("debug");
  HaxeMetadataTypeName DEPRECATED = new HaxeMetadataTypeName("deprecated");
  HaxeMetadataTypeName ENUM = new HaxeMetadataTypeName("enum");
  HaxeMetadataTypeName EXPOSE = new HaxeMetadataTypeName("expose"); // <name>
  HaxeMetadataTypeName FAKE_ENUM = new HaxeMetadataTypeName("fakeEnum");
  HaxeMetadataTypeName FILE = new HaxeMetadataTypeName("file"); // <File path>
  HaxeMetadataTypeName FINAL = new HaxeMetadataTypeName("final");
  HaxeMetadataTypeName FONT = new HaxeMetadataTypeName("font"); // <TTF PATH>, <Range String>
  HaxeMetadataTypeName FORWARD = new HaxeMetadataTypeName("forward"); // <List of field names>
  HaxeMetadataTypeName FORWARD_STATICS = new HaxeMetadataTypeName("forwardStatics"); // <List of field names>
  HaxeMetadataTypeName FROM = new HaxeMetadataTypeName("from");
  HaxeMetadataTypeName GENERIC = new HaxeMetadataTypeName("generic");
  HaxeMetadataTypeName GENERIC_BUILD = new HaxeMetadataTypeName("genericBuild");
  HaxeMetadataTypeName GETTER = new HaxeMetadataTypeName("getter"); // <Class field name>
  HaxeMetadataTypeName HACK = new HaxeMetadataTypeName("hack");
  HaxeMetadataTypeName HL_NATIVE = new HaxeMetadataTypeName("hlNative");
  HaxeMetadataTypeName IF_FEATURE = new HaxeMetadataTypeName("ifFeature"); // <Feature name>
  HaxeMetadataTypeName INLINE = new HaxeMetadataTypeName("inline");
  HaxeMetadataTypeName IS_VAR = new HaxeMetadataTypeName("isVar");
  HaxeMetadataTypeName JS_REQUIRE = new HaxeMetadataTypeName("jsRequire"); // <Module, [Dotted Path to Object]>
  HaxeMetadataTypeName KEEP = new HaxeMetadataTypeName("keep");
  HaxeMetadataTypeName KEEP_INIT = new HaxeMetadataTypeName("keepInit");
  HaxeMetadataTypeName KEEP_SUB = new HaxeMetadataTypeName("keepSub");
  HaxeMetadataTypeName MARKUP = new HaxeMetadataTypeName("markup");
  HaxeMetadataTypeName MACRO = new HaxeMetadataTypeName("macro"); // deprecated
  HaxeMetadataTypeName MERGE_BLOCK = new HaxeMetadataTypeName("mergeBlock");
  HaxeMetadataTypeName META = new HaxeMetadataTypeName("meta");
  HaxeMetadataTypeName MULTI_TYPE = new HaxeMetadataTypeName("multiType"); // <Relevant type parameters>
  HaxeMetadataTypeName NATIVE = new HaxeMetadataTypeName("native"); // <Output type path>
  HaxeMetadataTypeName NO_COMPLETION = new HaxeMetadataTypeName("noCompletion");
  HaxeMetadataTypeName NODEBUG = new HaxeMetadataTypeName("noDebug");
  HaxeMetadataTypeName NO_DOC = new HaxeMetadataTypeName("noDoc");
  HaxeMetadataTypeName NO_IMPORT_GLOBAL = new HaxeMetadataTypeName("noImportGlobal");
  HaxeMetadataTypeName NO_PRIVATE_ACCESS = new HaxeMetadataTypeName("noPrivateAccess");
  HaxeMetadataTypeName NOT_NULL = new HaxeMetadataTypeName("notNull");
  HaxeMetadataTypeName NO_USING = new HaxeMetadataTypeName("noUsing");
  HaxeMetadataTypeName NULL_SAFETY = new HaxeMetadataTypeName("nullSafety"); // <Off | Loose | Strict>
  HaxeMetadataTypeName NS = new HaxeMetadataTypeName("ns");
  HaxeMetadataTypeName OP = new HaxeMetadataTypeName("op");  // <The operation>
  HaxeMetadataTypeName OVERLOAD = new HaxeMetadataTypeName("overload");
  HaxeMetadataTypeName PERSISTENT = new HaxeMetadataTypeName("persistent");
  HaxeMetadataTypeName POS = new HaxeMetadataTypeName("pos"); // <Position>
  HaxeMetadataTypeName PRIVATE = new HaxeMetadataTypeName("private");
  HaxeMetadataTypeName PRIVATE_ACCESS = new HaxeMetadataTypeName("privateAccess");
  HaxeMetadataTypeName PROTECTED = new HaxeMetadataTypeName("protected");
  HaxeMetadataTypeName PUBLIC_FIELDS = new HaxeMetadataTypeName("publicFields");
  HaxeMetadataTypeName PURE = new HaxeMetadataTypeName("pure");
  HaxeMetadataTypeName REMOVE = new HaxeMetadataTypeName("remove");
  HaxeMetadataTypeName REQUIRE = new HaxeMetadataTypeName("require"); // <compiler flag to check>
  HaxeMetadataTypeName RESOLVE = new HaxeMetadataTypeName("resolve");
  HaxeMetadataTypeName RTTI = new HaxeMetadataTypeName("rtti");
  HaxeMetadataTypeName RUNTIME_VALUE = new HaxeMetadataTypeName("runtimeValue");
  HaxeMetadataTypeName SEMANTICS = new HaxeMetadataTypeName("semantics"); // <value | reference | variable>
  HaxeMetadataTypeName SETTER = new HaxeMetadataTypeName("setter"); // <Class field name>
  HaxeMetadataTypeName STRICT = new HaxeMetadataTypeName("strict");
  HaxeMetadataTypeName STRUCT_INIT = new HaxeMetadataTypeName("structInit");
  HaxeMetadataTypeName TO = new HaxeMetadataTypeName("to");
  HaxeMetadataTypeName UNIFY_MIN_DYNAMIC = new HaxeMetadataTypeName("unifyMinDynamic");
  HaxeMetadataTypeName UNREFLECTIVE = new HaxeMetadataTypeName("unreflective");
  HaxeMetadataTypeName USING = new HaxeMetadataTypeName("using");
  HaxeMetadataTypeName VALUE = new HaxeMetadataTypeName("value");

  HaxeMetadataTypeName CALLABLE = new HaxeMetadataTypeName("callable");

  // We need a token for things we don't know about (user meta).  This name cannot be constructed in code.
  // XXX: This needs to be thought through some more.  CUSTOM should not match just any string.
  HaxeMetadataTypeName CUSTOM = new HaxeMetadataTypeName("{custom}");
}
