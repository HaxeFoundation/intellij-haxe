package com.intellij.plugins.haxe.lang.psi.indexes.filebased.externalizer;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HaxeComponentExternalizer implements DataExternalizer<HaxeComponentIndexData> {

    public static final  HaxeComponentExternalizer INSTANCE = new HaxeComponentExternalizer();

    @Override
    public void save(@NotNull DataOutput out, HaxeComponentIndexData value) throws IOException {
        HaxeFqnExternalizer.INSTANCE.save(out, value.getFqn());
        IOUtil.writeUTF(out, value.getName());
        out.writeInt(value.getType().getKey());
        IOUtil.writeStringList(out, value.getTargets());
        out.writeBoolean(value.isPublic());
    }

    @Override
    public HaxeComponentIndexData read(@NotNull DataInput in) throws IOException {
        HaxeComponentIndexData data = new HaxeComponentIndexData();
        read(in, data);
        return data;
    }

    public HaxeComponentIndexData read(@NotNull DataInput in, HaxeComponentIndexData data) throws IOException {
        data.setFqn(HaxeFqnExternalizer.INSTANCE.read(in));
        data.setName(IOUtil.readUTF(in));
        data.setType(HaxeComponentType.valueOf(in.readInt()));
        data.setTargets(IOUtil.readStringList(in));
        data.setPublic(in.readBoolean());
        return data;
    }
}
