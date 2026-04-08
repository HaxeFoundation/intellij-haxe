package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeSuperClassStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Shared IStubElementType for all class-like declarations (class, interface, enum, abstract, typedef, extern*, macro*).
 * Parameterized by debug name and PSI creator function.
 */
public class HaxeClassStubElementType extends IStubElementType<HaxeClassStub, HaxeClass> {

  private final BiFunction<HaxeClassStub, HaxeClassStubElementType, HaxeClass> myPsiCreator;

  public HaxeClassStubElementType(@NotNull String debugName,
                                   @NotNull BiFunction<HaxeClassStub, HaxeClassStubElementType, HaxeClass> psiCreator) {
    super(debugName, HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.class." + super.getDebugName();
  }

  @Override
  public HaxeClass createPsi(@NotNull HaxeClassStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeClassStub createStub(@NotNull HaxeClass psi, StubElement parentStub) {
    String name = psi.getName();
    //  currently using  getQualifiedName() (not getFullyQualifiedName()) so that the stored FQN matches
    // the format that callers of findClassByQName() expect: "pkg.ClassName" for primary
    // classes (where filename == classname), and "pkg.Module.ClassName" for ancillary ones.
    // getFullyQualifiedName() always includes the module segment, producing
    // "pkg.SimpleClass.SimpleClass".
    // TODO:  consider  using getFullyQualifiedName and change resolve to always include module?
    String qualifiedName = psi.getQualifiedName();

    // TODO add info about macro expression (we dont want index on macro types)

    var componentType = com.intellij.plugins.haxe.HaxeComponentType.typeOf(psi);
    int componentTypeKey = componentType != null ? componentType.getKey() : -1;
    boolean isPrivate = !psi.isPublic();
    boolean isExtern = psi.isExtern();
    boolean isEnum = hasEnumPsiElement();

    // Collect super type names from extends/implements without resolving references.
    // IMPORTANT: typedefs implementation of getHaxeExtendsList() resolves through to the target class,
    // which triggers index access  which is illegal during stub creation/indexing.
    //TODO consider making stub for type & anonymous ?
    List<String> superNames = new ArrayList<>();
    if (psi instanceof HaxeTypedefDeclaration typedefDecl) {
      HaxeTypeOrAnonymous typeOrAnonymous = typedefDecl.getTypeOrAnonymous();
      if (typeOrAnonymous != null) {
        HaxeType type = typeOrAnonymous.getType();
        if (type != null) {
          String typeName = getUnresolvedTypeName(type);
          if (typeName != null) superNames.add(typeName);
        }else {
          // TODO anonymous type stub maybe ?
          //  + collect supers from anonymous type
        }
      }
    } else {
      for (HaxeType type : psi.getHaxeExtendsList()) {
        String typeName = getUnresolvedTypeName(type);
        if (typeName != null) superNames.add(typeName);
      }
      for (HaxeType type : psi.getHaxeImplementsList()) {
        String typeName = getUnresolvedTypeName(type);
        if (typeName != null) superNames.add(typeName);
      }
    }

    // Capture relevant/frequently used compile-time metadata
    int metaFlags = buildMetaFlags(psi);

    String[] superNamesArray = superNames.toArray(new String[0]);

    return new HaxeClassStub(parentStub, this, name,
                             qualifiedName, componentTypeKey,
                             isPrivate, isExtern, isEnum,
                             superNamesArray, metaFlags);
  }

  private boolean hasEnumPsiElement() {
    return this instanceof HaxeAbstractTypeDeclaration declaration &&
           declaration.getAbstractClassType().getFirstChild().textMatches("enum");
  }

  /** Walks preceding sibling metadata and packs the result into a metaFlags bitmask. */
  private static int buildMetaFlags(@NotNull HaxeClass psi) {
    var metas = HaxeMetadataUtils.getMetadataList(psi, HaxeMeta.COMPILE_TIME);
    int flags = 0;
    for (HaxeMeta meta : metas) {
      if (meta.isType(HaxeMeta.FINAL))         flags |= HaxeClassStub.META_FINAL;
      if (meta.isType(HaxeMeta.NATIVE))        flags |= HaxeClassStub.META_NATIVE;
      if (meta.isType(HaxeMeta.DEPRECATED))    flags |= HaxeClassStub.META_DEPRECATED;
      if (meta.isType(HaxeMeta.NO_COMPLETION)) flags |= HaxeClassStub.META_NO_COMPLETION;
      if (meta.isType(HaxeMeta.KEEP))          flags |= HaxeClassStub.META_KEEP;
      if (meta.isType(HaxePsiModifier.ABSTRACT)) flags |= HaxeClassStub.META_ABSTRACT;
    }
    return flags;
  }

  @Nullable
  private static String getUnresolvedTypeName(@NotNull HaxeType type) {
    // Get the text of the reference expression without resolving (used for typedefs as getHaxeExtendsList resolves)
    var refExpr = type.getReferenceExpression();
    return refExpr != null ? refExpr.getText() : null;
  }

  @Override
  public void serialize(@NotNull HaxeClassStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeName(stub.getQualifiedName());
    dataStream.writeVarInt(stub.getComponentTypeKey());
    dataStream.writeBoolean(stub.isPrivate());
    dataStream.writeBoolean(stub.isExtern());
    dataStream.writeBoolean(stub.isEnum());
    String[] superTypeNames = stub.getSuperTypeNames();
    dataStream.writeVarInt(superTypeNames.length);
    for (String superName : superTypeNames) {
      dataStream.writeName(superName);
    }
    dataStream.writeVarInt(stub.getMetaFlags());
    if (stub.getComponentTypeKey() == HaxeComponentType.TYPEDEF.getKey()) {
      // TODO add type ? function or  class?
    }
  }

  @NotNull
  @Override
  public HaxeClassStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    StringRef qualifiedNameRef = dataStream.readName();
    int componentTypeKey = dataStream.readVarInt();
    boolean isPrivate = dataStream.readBoolean();
    boolean isExtern = dataStream.readBoolean();
    boolean isEnum = dataStream.readBoolean();
    int superCount = dataStream.readVarInt();
    String[] superTypeNames = new String[superCount];
    for (int i = 0; i < superCount; i++) {
      StringRef ref = dataStream.readName();
      superTypeNames[i] = ref != null ? ref.getString() : "";
    }
    int metaFlags = dataStream.readVarInt();

    if (componentTypeKey == HaxeComponentType.TYPEDEF.getKey()) {
        // TODO add type ? function or  class?
    }

    String name = nameRef != null ? nameRef.getString() : null;
    String qualifiedName = qualifiedNameRef != null ? qualifiedNameRef.getString() : null;

    return new HaxeClassStub(parentStub, this, name, qualifiedName, componentTypeKey, isPrivate, isExtern, isEnum, superTypeNames, metaFlags);
  }

  @Override
  public void indexStub(@NotNull HaxeClassStub stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    if (name != null) {
      sink.occurrence(com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex.KEY, name);
    }
    String qualifiedName = stub.getQualifiedName();
    if (qualifiedName != null) {
      sink.occurrence(HaxeFullyQualifiedNameStubIndex.KEY, qualifiedName);
    }
    for (String superName : stub.getSuperTypeNames()) {
      if (superName != null && !superName.isEmpty()) {
        sink.occurrence(HaxeSuperClassStubIndex.KEY, superName);
      }
    }
  }
}

