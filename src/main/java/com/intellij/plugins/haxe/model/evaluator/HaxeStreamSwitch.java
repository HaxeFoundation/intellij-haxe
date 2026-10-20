package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * hxparse's stream DSL. In a class implementing {@code hxparse.ParserBuilder},
 * {@code switch stream { case [p1, p2]: ... }} matches the parser's next
 * tokens against the array's patterns in sequence, and a capture with a call
 * as initializer ({@code case [t, e = expr()]}) binds the call's result; the
 * build macro rewrites the switch into peek/junk calls. The patterns
 * therefore match a list of the parser's token type, not the stream field's
 * own type.
 */
public final class HaxeStreamSwitch {
  private static final String STREAM_FIELD = "stream";
  private static final String PARSER_BUILDER = "hxparse.ParserBuilder";
  private static final String PARSER = "hxparse.Parser";
  /** {@code hxparse.Parser<S, Token>}: the token type is the second argument. */
  private static final int TOKEN_ARGUMENT = 1;
  private static final int MAX_EXTENDS_DEPTH = 32;

  private HaxeStreamSwitch() {
  }

  /** Whether the switch is a stream switch of a parser-builder class. */
  public static boolean isStreamSwitch(@NotNull HaxeSwitchStatement switchStatement) {
    if (!(switchStatement.getExpression() instanceof HaxeReferenceExpression subject) || !subject.textMatches(STREAM_FIELD)) return false;
    HaxeClassModel owner = HaxeClassModel.fromElement(switchStatement);
    return owner != null && owner.getImplementsList().stream().anyMatch(type -> isClass(type, PARSER_BUILDER));
  }

  /** The token list the patterns match ({@code Array<Token>} for the parser's token type); null when the class does not extend hxparse.Parser. */
  @Nullable
  public static ResultHolder tokenListType(@NotNull HaxeSwitchStatement switchStatement) {
    HaxeType parserType = parserSuperType(HaxeClassModel.fromElement(switchStatement));
    ResultHolder token = parserType == null ? null : typeArgument(parserType, TOKEN_ARGUMENT);
    if (token == null || token.isUnknown()) return null;
    return SpecificTypeReference.getStdClass(SpecificTypeReference.ARRAY, switchStatement, new ResultHolder[]{token}).createHolder();
  }

  /** The class's {@code extends hxparse.Parser<...>} clause, searched up the extends chain. */
  @Nullable
  private static HaxeType parserSuperType(@Nullable HaxeClassModel classModel) {
    HaxeClassModel model = classModel;
    for (int depth = 0; model != null && depth < MAX_EXTENDS_DEPTH; depth++) {
      for (HaxeType type : model.getExtendsList()) {
        if (isClass(type, PARSER)) return type;
      }
      model = model.getParentClass();
    }
    return null;
  }

  private static boolean isClass(@NotNull HaxeType type, @NotNull String qualifiedName) {
    HaxeClass resolved = HaxeResolveUtil.tryResolveClassByQName(type);
    return resolved != null && qualifiedName.equals(resolved.getQualifiedName());
  }

  @Nullable
  private static ResultHolder typeArgument(@NotNull HaxeType type, int index) {
    HaxeTypeParam typeParam = type.getTypeParam();
    List<HaxeTypeListPart> arguments = typeParam == null ? List.of() : typeParam.getTypeList();
    if (arguments.size() <= index) return null;
    HaxeTypeOrAnonymous argument = arguments.get(index).getTypeOrAnonymous();
    return argument == null ? null : HaxeTypeResolver.getTypeFromTypeOrAnonymous(argument);
  }
}
