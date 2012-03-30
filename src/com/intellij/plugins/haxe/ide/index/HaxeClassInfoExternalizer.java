package com.intellij.plugins.haxe.ide.index;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.util.io.DataExternalizer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassInfoExternalizer implements DataExternalizer<HaxeClassInfo> {
  @Override
  public void save(DataOutput out, HaxeClassInfo classInfo) throws IOException {
    out.writeUTF(classInfo.getValue());
    final HaxeComponentType haxeComponentType = classInfo.getType();
    final int key = haxeComponentType == null ? -1 : haxeComponentType.getKey();
    out.writeInt(key);
  }

  @Override
  public HaxeClassInfo read(DataInput in) throws IOException {
    final String value = in.readUTF();
    final int key = in.readInt();
    return new HaxeClassInfo(value, HaxeComponentType.valueOf(key));
  }
}
