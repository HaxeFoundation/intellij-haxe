package com.intellij.plugins.haxe.lang.psi.indexes.filebased.externalizer;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HaxeComponentListExternalizer implements DataExternalizer<List<HaxeComponentIndexData>> {

    @Override
    public void save(@NotNull DataOutput out, List<HaxeComponentIndexData> values) throws IOException {
        out.writeInt(values.size());
        for (HaxeComponentIndexData data : values) {
            HaxeComponentExternalizer.INSTANCE.save(out, data);
        }
    }

    @Override
    public List<HaxeComponentIndexData> read(@NotNull DataInput in) throws IOException {
        final int size = in.readInt();
        final List<HaxeComponentIndexData> result = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            HaxeComponentIndexData indexData = HaxeComponentExternalizer.INSTANCE.read(in);
            result.add(indexData);
        }
        return result;
    }
}
