package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.HaxeCompilerMetadata;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.LightStubElementFactory;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


public class HaxeClassStubFactory implements StubElementFactory<HaxeClassStub, HaxeClass> {

    private final HaxeElementType myElementType;
    private final BiFunction<HaxeClassStub, HaxeElementType, HaxeClass> myPsiCreator;

    public HaxeClassStubFactory(@NotNull HaxeElementType elementType,
                                @NotNull BiFunction<HaxeClassStub, HaxeElementType, HaxeClass> psiCreator) {
        myElementType = elementType;
        this.myPsiCreator = psiCreator;
    }


    @Override
    public HaxeClass createPsi(@NotNull HaxeClassStub stub) {
        return myPsiCreator.apply(stub, myElementType);
    }

    @NotNull
    @Override
    public HaxeClassStub createStub(@NotNull HaxeClass psi, StubElement parentStub) {
        String name = psi.getName();
        String fullyQualifiedName = psi.getFullyQualifiedName();

        // TODO add info about macro expression (we dont want index on macro types)

        var componentType = com.intellij.plugins.haxe.HaxeComponentType.typeOf(psi);
        int componentTypeKey = componentType != null ? componentType.getKey() : -1;
        boolean isPrivate = !psi.isPublic();
        boolean isExtern = psi.isExtern();
        boolean isEnum = hasEnumPsiElement(psi);

        // Collect super type names from extends/implements without resolving references.
        // IMPORTANT: typedefs implementation of getHaxeExtendsList() resolves through to the target class,
        // which triggers index access  which is illegal during stub creation/indexing.
        List<String> superNames = new ArrayList<>();
        if (psi instanceof HaxeTypedefDeclaration typedefDecl) {
            HaxeTypeOrAnonymous typeOrAnonymous = typedefDecl.getTypeOrAnonymous();
            if (typeOrAnonymous != null) {
                HaxeType type = typeOrAnonymous.getType();
                if (type != null) {
                    String typeName = getUnresolvedTypeName(type);
                    if (typeName != null) superNames.add(typeName);
                }else {
                    // Anonymous type supers are collected when the anonymous type stub itself is created below.
                }
            }
        } else if (psi instanceof HaxeAnonymousType anonymousType) {
            // Collect composite type names (intersection types: TypeA & TypeB & { ... })
            for (HaxeType type : anonymousType.getTypeList()) {
                String typeName = getUnresolvedTypeName(type);
                if (typeName != null) superNames.add(typeName);
            }
            // Collect extension types ({> TypeA, > TypeB, ...fields})
            for (HaxeAnonymousTypeBody body : anonymousType.getAnonymousTypeBodyList()) {
                HaxeTypeExtendsList extendsList = body.getTypeExtendsList();
                if (extendsList != null) {
                    for (HaxeType type : extendsList.getTypeList()) {
                        String typeName = getUnresolvedTypeName(type);
                        if (typeName != null) superNames.add(typeName);
                    }
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

        return new HaxeClassStub(parentStub, myElementType, name,
                fullyQualifiedName, componentTypeKey,
                isPrivate, isExtern, isEnum,
                superNamesArray, metaFlags);
    }

    private boolean hasEnumPsiElement(@NotNull HaxeClass psi) {
        return psi instanceof HaxeAbstractTypeDeclaration declaration &&
                declaration.getAbstractClassType().getFirstChild().textMatches("enum");
    }

    /** Walks preceding sibling metadata and packs the result into a metaFlags bitmask. */
    private static int buildMetaFlags(@NotNull HaxeClass psi) {
        var metas = HaxeMetadataUtils.getMetadataList(psi, HaxeMeta.COMPILE_TIME);
        int flags = 0;
        for (HaxeMeta meta : metas) {
            if (meta.isType(HaxeCompilerMetadata.FINAL))           flags |= HaxeClassStub.META_FINAL;
            if (meta.isType(HaxeCompilerMetadata.NATIVE))          flags |= HaxeClassStub.META_NATIVE;
            if (meta.isType(HaxeCompilerMetadata.DEPRECATED))      flags |= HaxeClassStub.META_DEPRECATED;
            if (meta.isType(HaxeCompilerMetadata.NO_COMPLETION))   flags |= HaxeClassStub.META_NO_COMPLETION;
            if (meta.isType(HaxeCompilerMetadata.KEEP))            flags |= HaxeClassStub.META_KEEP;
            if (meta.isType(HaxeCompilerMetadata.ENUM))            flags |= HaxeClassStub.META_ENUM;
            if (meta.isType(HaxeCompilerMetadata.ABSTRACT))        flags |= HaxeClassStub.META_ABSTRACT;
            if (meta.isType(HaxeCompilerMetadata.GENERIC_BUILD))   flags |= HaxeClassStub.META_GENERIC_BUILD;
            if (meta.isType(HaxeCompilerMetadata.STRUCT_INIT))     flags |= HaxeClassStub.META_STRUCT_INIT;
            if (meta.isType(HaxeCompilerMetadata.USING))           flags |= HaxeClassStub.META_USING;
            if (meta.isType(HaxeCompilerMetadata.FORWARD))         flags |= HaxeClassStub.META_FORWARD;
            if (meta.isType(HaxeCompilerMetadata.CALLABLE))        flags |= HaxeClassStub.META_CALLABLE;
            if (meta.isType(HaxeCompilerMetadata.PUBLIC_FIELDS))   flags |= HaxeClassStub.META_PUBLIC_FIELDS;
        }
        return flags;
    }

    @Nullable
    private static String getUnresolvedTypeName(@NotNull HaxeType type) {
        // Get the text of the reference expression without resolving (used for typedefs as getHaxeExtendsList resolves)
        var refExpr = type.getReferenceExpression();
        return refExpr != null ? refExpr.getText() : null;
    }

}
