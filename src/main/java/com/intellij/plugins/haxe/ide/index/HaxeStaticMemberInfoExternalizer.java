package com.intellij.plugins.haxe.ide.index;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;


public class HaxeStaticMemberInfoExternalizer implements DataExternalizer<HaxeStaticMemberInfo> {

  private final ThreadLocal<byte[]> buffer = ThreadLocal.withInitial(IOUtil::allocReadWriteUTFBuffer);

  @Override
  public void save(@NotNull DataOutput out, HaxeStaticMemberInfo memberInfo) throws IOException {
    IOUtil.writeUTFFast(buffer.get(), out, memberInfo.getPackageName());
    IOUtil.writeUTFFast(buffer.get(), out, memberInfo.getModuleName());
    IOUtil.writeUTFFast(buffer.get(), out, Optional.ofNullable(memberInfo.getClassName()).orElse(""));
    IOUtil.writeUTFFast(buffer.get(), out, memberInfo.getMemberName());
    IOUtil.writeUTFFast(buffer.get(), out, memberInfo.getTypeValue());
    final HaxeComponentType haxeComponentType = memberInfo.getType();
    out.writeInt( haxeComponentType.getKey());
  }

  @Override
  public HaxeStaticMemberInfo read(@NotNull DataInput in) throws IOException {
    final String ownerPackage = IOUtil.readUTFFast(buffer.get(), in);
    final String moduleName = IOUtil.readUTFFast(buffer.get(), in);
    final String className = IOUtil.readUTFFast(buffer.get(), in);
    final String memberName = IOUtil.readUTFFast(buffer.get(), in);
    final String typeValue = IOUtil.readUTFFast(buffer.get(), in);

    HaxeComponentType type = HaxeComponentType.valueOf(in.readInt());
    if (type == null) type = HaxeComponentType.FIELD;
    return new HaxeStaticMemberInfo(ownerPackage, moduleName, Objects.equals(className, "") ? null : className , memberName, type, typeValue);
  }
}
