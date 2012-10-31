package com.intellij.plugins.haxe.ide.index;

import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassInfoListExternalizer implements DataExternalizer<List<HaxeClassInfo>> {
  private final byte[] buffer = IOUtil.allocReadWriteUTFBuffer();

  @Override
  public void save(DataOutput out, List<HaxeClassInfo> value) throws IOException {
    out.writeInt(value.size());
    for (HaxeClassInfo classInfo : value) {
      final HaxeComponentType haxeComponentType = classInfo.getType();
      final int key = haxeComponentType == null ? -1 : haxeComponentType.getKey();
      out.writeInt(key);
      IOUtil.writeUTFFast(buffer, out, classInfo.getValue());
    }
  }

  @Override
  public List<HaxeClassInfo> read(DataInput in) throws IOException {
    final int size = in.readInt();
    final List<HaxeClassInfo> result = new ArrayList<HaxeClassInfo>(size);
    for (int i = 0; i < size; ++i) {
      final int key = in.readInt();
      final String value = IOUtil.readUTFFast(buffer, in);
      result.add(new HaxeClassInfo(value, HaxeComponentType.valueOf(key)));
    }
    return result;
  }
}
