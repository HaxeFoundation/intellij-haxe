package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.display.client.DisplayJson;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.MissingFields;
import com.intellij.psi.PsiDocumentManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The fixes built from captured haxe 4.3.7 diagnostics (trimmed), applied
 * to files shaped like the ones the diagnostics were captured on.
 */
@DisplayName("Compiler diagnostics: quick fixes")
public class HaxeCompilerDiagnosticFixesTest extends HaxeLightFixtureTestCase {

  private static final String INTERFACE_SOURCE = """
    interface Greeter { function greet(name:String):String; var id(get, never):Int; }
    class Live implements Greeter {
    	static function main() {}
    }
    """;
  private static final String FINAL_FIELDS_SOURCE = """
    class Holder {
    	final x:Int;
    	final label:String;
    }
    """;
  private static final String UNKNOWN_CALL_SOURCE = """
    class Live {
    	static function main() { trce("x"); }
    }
    """;
  private static final String UNRESOLVED_TYPE_SOURCE = """
    class Live {
    	static function main() { var m:StringMap<Int> = null; }
    }
    """;

  /** Diagnostics 0-3: interface members, final fields, unknown call, unresolved type (its range fits UNRESOLVED_TYPE_SOURCE). */
  private static final String PAYLOAD = """
    {"jsonrpc":"2.0","id":1,"result":{"result":[{"file":"C:\\\\work\\\\Live.hx","diagnostics":[
      {"kind":7,"severity":1,"range":{"start":{"line":1,"character":6},"end":{"line":1,"character":10}},
       "args":{"moduleType":{"kind":"class","pack":[],"name":"Live","moduleName":"Live"},"moduleFile":"C:\\\\work\\\\Live.hx",
         "entries":[{"fields":[
           {"field":{"name":"get_id","type":{"kind":"TFun","args":{"args":[],"ret":{"kind":"TAbstract","args":{"path":{"typeName":"Int","moduleName":"StdTypes","pack":[]},"params":[]}}}},"isPublic":true,"kind":{"kind":"FMethod","args":"MethNormal"},"scope":1},"unique":true},
           {"field":{"name":"greet","type":{"kind":"TFun","args":{"args":[{"name":"name","opt":false,"t":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}}}],"ret":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}}}},"isPublic":true,"kind":{"kind":"FMethod","args":"MethNormal"},"scope":1},"unique":true},
           {"field":{"name":"id","type":{"kind":"TAbstract","args":{"path":{"typeName":"Int","moduleName":"StdTypes","pack":[]},"params":[]}},"isPublic":true,"kind":{"kind":"FVar","args":{"read":{"kind":"AccCall"},"write":{"kind":"AccNever"}}},"scope":1},"unique":true}],
           "cause":{"kind":"ImplementedInterface","args":{"parent":{"path":{"typeName":"Greeter","moduleName":"Live","pack":[]},"params":[]}}}}]},
       "relatedInformation":[]},
      {"kind":7,"severity":1,"range":{"start":{"line":0,"character":6},"end":{"line":0,"character":12}},
       "args":{"moduleType":{"kind":"class","pack":[],"name":"Holder","moduleName":"Live"},"moduleFile":"C:\\\\work\\\\Live.hx",
         "entries":[{"fields":[],"cause":{"kind":"FinalFields","args":{"fields":[
           {"name":"label","type":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}},"isPublic":false,"kind":{"kind":"FVar","args":{"read":{"kind":"AccNormal"},"write":{"kind":"AccCtor"}}},"pos":{"file":"Live.hx","min":30,"max":49},"scope":1},
           {"name":"x","type":{"kind":"TAbstract","args":{"path":{"typeName":"Int","moduleName":"StdTypes","pack":[]},"params":[]}},"isPublic":false,"kind":{"kind":"FVar","args":{"read":{"kind":"AccNormal"},"write":{"kind":"AccCtor"}}},"pos":{"file":"Live.hx","min":16,"max":28},"scope":1}]}}}]},
       "relatedInformation":[]},
      {"kind":7,"severity":1,"range":{"start":{"line":1,"character":26},"end":{"line":1,"character":30}},
       "args":{"moduleType":{"kind":"class","pack":[],"name":"Live","moduleName":"Live"},"moduleFile":"C:\\\\work\\\\Live.hx",
         "entries":[{"fields":[
           {"field":{"name":"trce","type":{"kind":"TFun","args":{"args":[{"name":"arg0","opt":false,"t":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}}}],"ret":{"kind":"TMono"}}},"isPublic":false,"kind":{"kind":"FMethod","args":"MethNormal"},"scope":0},"unique":true}],
           "cause":{"kind":"FieldAccess","args":{}}}]},
       "relatedInformation":[]},
      {"kind":1,"severity":1,"range":{"start":{"line":1,"character":32},"end":{"line":1,"character":41}},
       "args":[{"kind":0,"name":"haxe.ds.StringMap"},{"kind":1,"name":"StringBuf"},{"kind":1,"name":"DOMStringMap"}],"relatedInformation":[]}
    ]}],"timestamp":1790550805.32}}""";

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("implements interface members from the compilers field list")
  public void testImplementsInterfaceMembersFromTheCompilersFieldList() {
    myFixture.configureByText("Live.hx", INTERFACE_SOURCE);
    MissingFields missing = diagnostic(0).missingFieldsArg();
    IntentionAction fix = new HaxeImplementMissingFieldsQuickFix(missing.typeName(), missing.entries().getFirst());
    assertEquals("Implement missing members", fix.getText());

    apply(fix);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("public function get_id():Int"), text);
    assertTrue(text.contains("public function greet(name:String):String"), text);
    assertTrue(text.contains("throw new haxe.exceptions.NotImplementedException();"), text);
    assertTrue(text.contains("public var id(get, never):Int;"), text);
    assertFalse(text.contains("override"), "an interface member is not an override:\n" + text);
  }

  @Test
  @DisplayName("creates a constructor for uninitialized final fields in declaration order")
  public void testCreatesAConstructorForUninitializedFinalFieldsInDeclarationOrder() {
    myFixture.configureByText("Holder.hx", FINAL_FIELDS_SOURCE);
    MissingFields missing = diagnostic(1).missingFieldsArg();
    IntentionAction fix = new HaxeImplementMissingFieldsQuickFix(missing.typeName(), missing.entries().getFirst());
    assertEquals("Create constructor initializing final fields", fix.getText());

    apply(fix);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("public function new(x:Int, label:String)"), text);
    assertTrue(text.contains("this.x = x;"), text);
    assertTrue(text.contains("this.label = label;"), text);
  }

  @Test
  @DisplayName("creates the function an unknown call names")
  public void testCreatesTheFunctionAnUnknownCallNames() {
    myFixture.configureByText("Live.hx", UNKNOWN_CALL_SOURCE);
    MissingFields missing = diagnostic(2).missingFieldsArg();
    IntentionAction fix = new HaxeImplementMissingFieldsQuickFix(missing.typeName(), missing.entries().getFirst());
    assertEquals("Create function 'trce'", fix.getText());

    apply(fix);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("static function trce(arg0:String):Dynamic"), text);
  }

  @Test
  @DisplayName("the fix does nothing once the class is gone")
  public void testTheFixDoesNothingOnceTheClassIsGone() {
    myFixture.configureByText("Other.hx", "class Other {}\n");
    MissingFields missing = diagnostic(2).missingFieldsArg();
    IntentionAction fix = new HaxeImplementMissingFieldsQuickFix(missing.typeName(), missing.entries().getFirst());

    boolean available = fix.isAvailable(getProject(), myFixture.getEditor(), myFixture.getFile());
    apply(fix);

    assertFalse(available);
    assertEquals("class Other {}\n", myFixture.getFile().getText());
  }

  @Test
  @DisplayName("offers an import and spelling corrections for an unresolved identifier")
  public void testOffersAnImportAndSpellingCorrectionsForAnUnresolvedIdentifier() {
    myFixture.configureByText("Live.hx", UNRESOLVED_TYPE_SOURCE);
    Diagnostic unresolved = diagnostic(3);
    Document document = myFixture.getEditor().getDocument();
    TextRange range = HaxeDiagnosticsFetcher.toTextRange(document, unresolved.range());
    assertEquals("StringMap", document.getText(range));

    List<IntentionAction> fixes = HaxeCompilerDiagnosticsAnnotator.compilerFixesFor(document, unresolved, range);

    List<String> labels = fixes.stream().map(IntentionAction::getText).toList();
    assertEquals(List.of("Import 'haxe.ds.StringMap'", "Change to 'StringBuf'", "Change to 'DOMStringMap'"), labels);
  }

  @Test
  @DisplayName("the import fix adds the suggested import once")
  public void testTheImportFixAddsTheSuggestedImportOnce() {
    myFixture.configureByText("Live.hx", UNRESOLVED_TYPE_SOURCE);
    IntentionAction fix = new HaxeCompilerImportQuickFix("haxe.ds.StringMap");
    assertTrue(fix.isAvailable(getProject(), myFixture.getEditor(), myFixture.getFile()));

    apply(fix);

    assertTrue(myFixture.getFile().getText().startsWith("import haxe.ds.StringMap;"), myFixture.getFile().getText());
    assertFalse(fix.isAvailable(getProject(), myFixture.getEditor(), myFixture.getFile()), "an existing import is not offered again");
  }

  @Test
  @DisplayName("the spelling fix replaces the identifier")
  public void testTheSpellingFixReplacesTheIdentifier() {
    myFixture.configureByText("Live.hx", UNRESOLVED_TYPE_SOURCE);
    Diagnostic unresolved = diagnostic(3);
    Document document = myFixture.getEditor().getDocument();
    TextRange range = HaxeDiagnosticsFetcher.toTextRange(document, unresolved.range());
    IntentionAction fix = HaxeCompilerDiagnosticsAnnotator.compilerFixesFor(document, unresolved, range).get(1);

    apply(fix);

    assertTrue(myFixture.getFile().getText().contains("var m:StringBuf<Int> = null;"), myFixture.getFile().getText());
  }

  private static Diagnostic diagnostic(int index) {
    try {
      return DisplayJson.decodeDiagnostics(DisplayJson.unwrap(PAYLOAD)).getFirst().diagnostics().get(index);
    }
    catch (Exception e) {
      throw new AssertionError("the fixture payload must decode", e);
    }
  }

  /** A document-editing fix leaves the PSI text behind until the document is committed. */
  private void apply(IntentionAction fix) {
    Runnable invoke = () -> {
      fix.invoke(getProject(), myFixture.getEditor(), myFixture.getFile());
      PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    };
    WriteCommandAction.runWriteCommandAction(getProject(), invoke);
  }
}
