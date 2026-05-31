package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeClassInheritanceStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeTypedefInheritanceStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class HaxeClassStubSerializer implements StubSerializer<HaxeClassStub> {

    private final IElementType myElementType;
    public HaxeClassStubSerializer(IElementType elementType) {
        myElementType = elementType;
    }

    @Override
    public @NotNull String getExternalId() {
        return "haxe.class." + myElementType;
    }


    @Override
    public void indexStub(@NotNull HaxeClassStub stub, @NotNull IndexSink sink) {
        if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(stub)) {
            return;
        }

        String name = stub.getName();
        // TODO filter types ?

        // ignore anonymous types (as they can be part of type tags, not to be confused with typedefs)
        if (name == null) return;

        sink.occurrence(HaxeClassNameStubIndex.KEY, name);

        // short Qname eliminates module name if its the same as class name
        String qualifiedNameShort = stub.getQualifiedName(false);
        String qualifiedNameFull = stub.getQualifiedName(true);

        if (qualifiedNameFull != null) {
            sink.occurrence(HaxeFullyQualifiedClassNameStubIndex.KEY, qualifiedNameFull);
        }
        if (qualifiedNameShort != null && !qualifiedNameShort.equals(qualifiedNameFull)) {
            sink.occurrence(HaxeFullyQualifiedClassNameStubIndex.KEY, qualifiedNameShort);
        }

        if(stub.getPsi().isTypeDef()) {
            for (String superName : stub.getSuperTypeNames()) {
                if (superName != null && !superName.isEmpty()) {
                    sink.occurrence(HaxeTypedefInheritanceStubIndex.KEY, superName);
                }
            }
        }else {
            for (String superName : stub.getSuperTypeNames()) {
                if (superName != null && !superName.isEmpty()) {
                    sink.occurrence(HaxeClassInheritanceStubIndex.KEY, superName);
                }
            }
        }
    }

    @Override
    public void serialize(@NotNull HaxeClassStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeName(stub.getFullyQualifiedName());
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



        String name = nameRef != null ? nameRef.getString() : null;
        String qualifiedName = qualifiedNameRef != null ? qualifiedNameRef.getString() : null;
        //TODO
        return new HaxeClassStub(parentStub, myElementType, name, qualifiedName, componentTypeKey, isPrivate, isExtern, isEnum, superTypeNames, metaFlags);
    }

}
