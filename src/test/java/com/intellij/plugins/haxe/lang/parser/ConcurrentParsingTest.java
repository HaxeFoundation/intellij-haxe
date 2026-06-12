package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.impl.DebugUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentParsingTest extends HaxeParsingTestBase {

  public ConcurrentParsingTest() {
    super("parsing", "haxe");
  }

  /**
   * Multi-token operators (>=, >>, >>= ...) are merged from single '>'/'=' tokens while parsing,
   * and whitespace between the tokens must prevent the merge. The IDE parses several files
   * concurrently (indexing, highlighting), so that bookkeeping must be per parse, never shared
   * between threads. This is a coarse determinism guard: it catches gross shared-state bugs,
   * but the original (rare) flake it relates to was never reproducible on demand.
   */
  @Test
  public void testConcurrentParsesProduceIdenticalTrees() throws Exception {
    StringBuilder victimBody = new StringBuilder();
    for (int i = 0; i < 30; i++) {
      victimBody.append("    var cb").append(i).append(":Null<()->(()->Void)> = null;\n");
      victimBody.append("    var n").append(i).append(":Null<Int> = ").append(i).append(";\n");
    }
    String victim = "class A {\n  static function main() {\n" + victimBody + "  }\n}\n";

    StringBuilder aggressorBody = new StringBuilder();
    for (int i = 0; i < 50; i++) {
      aggressorBody.append("    if (a > ").append(i).append(") b = a >= ").append(i).append(" ? a >> 1 : a >>> 2;\n");
    }
    String aggressor = "class B {\n  static function f(a:Int, b:Int) {\n" + aggressorBody + "  }\n}\n";

    String expected = parseToTreeText(victim);

    int threadCount = 4;
    int iterations = 100;
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (int t = 0; t < threadCount; t++) {
        boolean isVictim = t % 2 == 0;
        futures.add(pool.submit((Callable<Integer>)() -> {
          int mismatches = 0;
          for (int i = 0; i < iterations; i++) {
            if (isVictim) {
              if (!expected.equals(parseToTreeText(victim))) mismatches++;
            }
            else {
              parseToTreeText(aggressor);
            }
          }
          return mismatches;
        }));
      }
      int mismatches = 0;
      for (Future<Integer> future : futures) {
        mismatches += future.get();
      }
      assertEquals("identical text must parse to an identical tree, also while other threads parse", 0, mismatches);
    }
    finally {
      pool.shutdownNow();
    }
  }

  private String parseToTreeText(String text) {
    HaxeParserDefinition definition = new HaxeParserDefinition();
    PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(definition, definition.createLexer(getProject()), text);
    ASTNode root = definition.createParser(getProject()).parse(HaxeTokenTypeSets.HAXE_FILE, builder);
    return DebugUtil.nodeTreeToString(root, true);
  }
}
