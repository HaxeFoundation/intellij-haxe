package com.intellij.plugins.haxe.display.protocol.server;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import java.util.List;

/**
 * The blueprint of one type: its shape after macros ran, as
 * {@code server/type} reports it. It lists every member with its type,
 * including macro-generated members that exist in no source file. Member
 * lookups that static resolution cannot answer fall back to it. Haxe 5 sends
 * the member types unresolved (see the README).
 */
public record TypeBlueprint(String name,
                            String kind,
                            List<Member> fields,
                            List<Member> statics) {

  /** {@code fieldKind} is the JsonClassField kind: FMethod, FVar or FProp. */
  public record Member(String name, JsonTypeRef type, String fieldKind) {
    public boolean isMethod() {
      return "FMethod".equals(fieldKind);
    }
  }

  /** The instance or static member named {@code memberName}; null when the type has none. */
  public Member findMember(String memberName) {
    for (Member member : fields) {
      if (member.name().equals(memberName)) return member;
    }
    for (Member member : statics) {
      if (member.name().equals(memberName)) return member;
    }
    return null;
  }
}
