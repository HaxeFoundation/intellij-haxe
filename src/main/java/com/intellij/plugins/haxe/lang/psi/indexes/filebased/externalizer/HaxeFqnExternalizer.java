package com.intellij.plugins.haxe.lang.psi.indexes.filebased.externalizer;

import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;

public class HaxeFqnExternalizer implements DataExternalizer<FullyQualifiedInfo> {

    public static final HaxeFqnExternalizer INSTANCE =  new HaxeFqnExternalizer();
    
    @Override
    public void save(@NotNull DataOutput out, FullyQualifiedInfo value) throws IOException {
        IOUtil.writeUTF(out, value.getPackageName());
        IOUtil.writeUTF(out, nullToEmpty(value.getModuleName()));
        IOUtil.writeUTF(out, nullToEmpty(value.getClassName()));
        IOUtil.writeUTF(out, nullToEmpty(value.getMemberName()));
        IOUtil.writeUTF(out, nullToEmpty(value.getParameterName()));
    }

    @Override
    public FullyQualifiedInfo read(@NotNull DataInput in) throws IOException {
        String packageName = IOUtil.readUTF(in);
        String moduleName = emptyToNull(IOUtil.readUTF(in));
        String className = emptyToNull(IOUtil.readUTF(in));
        String memberName = emptyToNull(IOUtil.readUTF(in));
        String parameterName = emptyToNull(IOUtil.readUTF(in));
        return new FullyQualifiedInfo(packageName, moduleName, className, memberName, parameterName);
    }
}
