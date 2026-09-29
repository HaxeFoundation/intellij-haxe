package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.*;
import com.intellij.plugins.haxe.display.protocol.server.HaxeServerContext;
import com.intellij.plugins.haxe.display.protocol.server.ModuleInfo;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decodes trimmed responses captured from a haxe 4.3.7 server. Wire ranges
 * are 0-BASED, and the assertions pin that down.
 */
@DisplayName("Display protocol: json decoding")
public class DisplayJsonTest {

  @Test
  @DisplayName("request envelope carries jsonrpc id method and params")
  public void requestEnvelopeCarriesJsonrpcIdMethodAndParams() {
    String request = DisplayJson.encodeRequest("display/hover", Map.of("offset", 42));
    assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"display/hover\",\"params\":{\"offset\":42}}", request);
  }

  @Test
  @DisplayName("json rpc error unwraps into an exception")
  public void jsonRpcErrorUnwrapsIntoAnException() {
    String payload = """
      {"jsonrpc":"2.0","id":null,"error":{"code":-32600,"message":"not an object"}}""";
    DisplayRequestException e = assertThrows(DisplayRequestException.class, () -> DisplayJson.unwrap(payload));
    assertTrue(e.getMessage().contains("not an object"));
  }

  @Test
  @DisplayName("completion decodes every named item kind with its detail and type")
  public void completionDecodesEveryNamedItemKindWithItsDetailAndType() throws Exception {
    // trimmed from a Toplevel-mode response: one item per kind the lookup shows, plus an
    // anonymous structure, which the decoder drops because it has no insert text
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":{"mode":{"kind":2,"args":{"expectedType":{"kind":"TType","args":{"path":{"pack":[],"moduleName":"Main","typeName":"Handler","importStatus":0},"params":[]}},"expectedTypeFollowed":{"kind":"TFun","args":{"args":[{"name":"","opt":false,"t":{"kind":"TAbstract","args":{"path":{"pack":[],"moduleName":"StdTypes","typeName":"Int","importStatus":0},"params":[]}}}],"ret":{"kind":"TAbstract","args":{"path":{"pack":[],"moduleName":"StdTypes","typeName":"Bool","importStatus":0},"params":[]}}}}}},"isIncomplete":true,
        "replaceRange":{"start":{"line":3,"character":8},"end":{"line":3,"character":10}},
        "items":[
          {"kind":"Local","args":{"id":1,"name":"count","type":{"kind":"TAbstract","args":{"path":{"pack":[],"moduleName":"StdTypes","typeName":"Int","importStatus":0},"params":[]}}},"index":0},
          {"kind":"ClassField","args":{"field":{"name":"shout","doc":"\\n\\t\\tShouts the label.\\n\\t","kind":{"kind":"FMethod","args":"MethNormal"}}},"type":{"kind":"TFun","args":{"args":[],"ret":{"kind":"TInst","args":{"path":{"pack":[],"moduleName":"String","typeName":"String","importStatus":0},"params":[]}}}},"index":1},
          {"kind":"EnumField","args":{"field":{"name":"Red"}},"index":2},
          {"kind":"Type","args":{"path":{"pack":["haxe","ds"],"moduleName":"StringMap","typeName":"StringMap","importStatus":1},"kind":"class"},"index":3},
          {"kind":"Package","args":{"path":{"pack":["haxe","ds"]}},"index":4},
          {"kind":"Module","args":{"path":{"pack":["haxe"],"moduleName":"Json"}},"index":5},
          {"kind":"Keyword","args":{"name":"var"},"index":6},
          {"kind":"Literal","args":{"name":"null"},"index":7},
          {"kind":"Metadata","args":{"name":":keep"},"index":8},
          {"kind":"Define","args":{"name":"debug"},"index":9},
          {"kind":"AnonymousStructure","args":{"fields":[]},"index":10}
        ]},"timestamp":1785367994.26}}""";

    CompletionList completion = DisplayJson.decodeCompletion(DisplayJson.unwrap(payload));

    assertEquals(2, completion.modeKind());
    assertTrue(completion.incomplete());
    assertTrue(completion.expectedType().isFunction(), "the followed expected type wins over the typedef");
    assertEquals("(Int) -> Bool", completion.expectedType().presentable());
    assertEquals(new Range(new Position(3, 8), new Position(3, 10)), completion.replaceRange());
    List<String> names = completion.items().stream().map(CompletionItem::name).toList();
    assertEquals(List.of("count", "shout", "Red", "StringMap", "ds", "Json", "var", "null", ":keep", "debug"), names);
    CompletionItem local = completion.items().get(0);
    assertTrue(local.isLocalOrTypeParameter());
    assertEquals("Int", local.type().presentable());
    CompletionItem method = completion.items().get(1);
    assertTrue(method.isField());
    assertTrue(method.type().isFunction());
    assertEquals("\n\t\tShouts the label.\n\t", method.doc());
    assertNull(local.doc(), "a local carries no doc");
    CompletionItem type = completion.items().get(3);
    assertEquals("haxe.ds.StringMap", type.detail());
    assertEquals("class", type.moduleTypeKind());
    assertEquals("haxe.ds", completion.items().get(4).detail());
    assertTrue(completion.items().get(6).isKeywordOrLiteral());
  }

  @Test
  @DisplayName("diagnostics decode with zero based ranges and typed args")
  public void diagnosticsDecodeWithZeroBasedRangesAndTypedArgs() throws Exception {
    // captured: unused import on file line 1, unresolved identifier on file line 7
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":[{"file":"C:\\\\work\\\\Main.hx","diagnostics":[
        {"kind":1,"severity":1,"range":{"start":{"line":6,"character":2},"end":{"line":6,"character":14}},
         "args":[{"kind":0,"name":"haxe.ds.StringMap"}],"relatedInformation":[]},
        {"kind":2,"severity":2,"range":{"start":{"line":6,"character":2},"end":{"line":6,"character":14}},
         "args":"This code has no effect","relatedInformation":[]},
        {"kind":0,"severity":2,"range":{"start":{"line":0,"character":0},"end":{"line":0,"character":25}},
         "args":[],"relatedInformation":[]}]}],"timestamp":1785367994.26}}""";

    List<FileDiagnostics> files = DisplayJson.decodeDiagnostics(DisplayJson.unwrap(payload));

    assertEquals(1, files.size());
    FileDiagnostics file = files.get(0);
    assertEquals("C:\\work\\Main.hx", file.file());
    assertEquals(3, file.diagnostics().size());

    Diagnostic unresolved = file.diagnostics().get(0);
    assertEquals(DiagnosticKind.UNRESOLVED_IDENTIFIER, unresolved.kind());
    assertEquals(DiagnosticSeverity.ERROR, unresolved.severity());
    assertEquals(new Range(new Position(6, 2), new Position(6, 14)), unresolved.range());
    assertEquals(List.of(new Diagnostic.IdentifierSuggestion(0, "haxe.ds.StringMap")),
                 unresolved.suggestionArgs());
    assertTrue(unresolved.suggestionArgs().get(0).isImportCandidate());

    Diagnostic warning = file.diagnostics().get(1);
    assertEquals(DiagnosticKind.COMPILER_ERROR, warning.kind());
    assertEquals("This code has no effect", warning.messageArg());

    assertEquals(DiagnosticKind.UNUSED_IMPORT, file.diagnostics().get(2).kind());
  }

  @Test
  @DisplayName("missing fields decode their cause and typed field lists")
  public void missingFieldsDecodeTheirCauseAndTypedFieldLists() throws Exception {
    // captured: a class implementing an interface with a method and a (get, never) property,
    // and a class whose two final fields no constructor initializes (trimmed)
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":[{"file":"C:\\\\work\\\\Live.hx","diagnostics":[
        {"kind":7,"severity":1,"range":{"start":{"line":4,"character":6},"end":{"line":4,"character":10}},
         "args":{"moduleType":{"kind":"class","pack":[],"name":"Live","moduleName":"Live"},"moduleFile":"C:\\\\work\\\\Live.hx",
           "entries":[{"fields":[
             {"field":{"name":"greet","type":{"kind":"TFun","args":{"args":[{"name":"name","opt":false,"t":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}}}],"ret":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}}}},"isPublic":true,"kind":{"kind":"FMethod","args":"MethNormal"},"pos":{"file":"Live.hx","min":20,"max":55},"scope":1},"unique":true},
             {"field":{"name":"id","type":{"kind":"TAbstract","args":{"path":{"typeName":"Int","moduleName":"StdTypes","pack":[]},"params":[]}},"isPublic":true,"kind":{"kind":"FVar","args":{"read":{"kind":"AccCall"},"write":{"kind":"AccNever"}}},"pos":{"file":"Live.hx","min":56,"max":79},"scope":1},"unique":true}],
             "cause":{"kind":"ImplementedInterface","args":{"parent":{"path":{"typeName":"Greeter","moduleName":"Live","pack":[]},"params":[]}}}}]},
         "relatedInformation":[]},
        {"kind":7,"severity":1,"range":{"start":{"line":3,"character":6},"end":{"line":3,"character":12}},
         "args":{"moduleType":{"kind":"class","pack":[],"name":"Holder","moduleName":"Live"},"moduleFile":"C:\\\\work\\\\Live.hx",
           "entries":[{"fields":[],"cause":{"kind":"FinalFields","args":{"fields":[
             {"name":"label","type":{"kind":"TInst","args":{"path":{"typeName":"String","moduleName":"String","pack":[]},"params":[]}},"isPublic":false,"isFinal":true,"kind":{"kind":"FVar","args":{"read":{"kind":"AccNormal"},"write":{"kind":"AccCtor"}}},"pos":{"file":"Live.hx","min":191,"max":210},"scope":1},
             {"name":"x","type":{"kind":"TAbstract","args":{"path":{"typeName":"Int","moduleName":"StdTypes","pack":[]},"params":[]}},"isPublic":false,"isFinal":true,"kind":{"kind":"FVar","args":{"read":{"kind":"AccNormal"},"write":{"kind":"AccCtor"}}},"pos":{"file":"Live.hx","min":178,"max":190},"scope":1}]}}}]},
         "relatedInformation":[]}]}],"timestamp":1790550805.32}}""";

    List<Diagnostic> diagnostics = DisplayJson.decodeDiagnostics(DisplayJson.unwrap(payload)).get(0).diagnostics();

    MissingFields implemented = diagnostics.get(0).missingFieldsArg();
    assertEquals("Live", implemented.typeName());
    MissingFields.Entry interfaceEntry = implemented.entries().get(0);
    assertEquals("ImplementedInterface", interfaceEntry.causeKind());
    MissingFields.MissingField greet = interfaceEntry.fields().get(0);
    assertTrue(greet.isMethod());
    assertTrue(greet.isPublic());
    assertFalse(greet.isStatic());
    assertEquals("(name:String) -> String", greet.type().presentable());
    MissingFields.MissingField id = interfaceEntry.fields().get(1);
    assertEquals("FVar", id.fieldKind());
    assertEquals("AccCall", id.readAccess());
    assertEquals("AccNever", id.writeAccess());

    MissingFields.Entry finals = diagnostics.get(1).missingFieldsArg().entries().get(0);
    assertTrue(finals.isFinalFields());
    assertTrue(finals.fields().isEmpty(), "the fields sit in the cause for final fields");
    List<String> finalNames = finals.causeFields().stream().map(MissingFields.MissingField::name).toList();
    assertEquals(List.of("label", "x"), finalNames);
    assertEquals(191, finals.causeFields().get(0).declarationOffset());
    assertNull(diagnostics.get(0).removableRangeArg(), "no removable range on a missing-fields entry");
  }

  @Test
  @DisplayName("hover decodes item kind and json type")
  public void hoverDecodesItemKindAndJsonType() throws Exception {
    // captured hover over a local String variable (trimmed)
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":{"documentation":null,
        "range":{"start":{"line":5,"character":8},"end":{"line":5,"character":16}},
        "item":{"kind":"Local","args":{"name":"greeting"},
          "type":{"kind":"TInst","args":{"path":{"pack":[],"moduleName":"String","typeName":"String"},"params":[]}}}},
        "timestamp":1785368029.61}}""";

    HoverInfo hover = DisplayJson.decodeHover(DisplayJson.unwrap(payload));

    assertNotNull(hover);
    assertEquals("Local", hover.itemKind());
    assertEquals(new Range(new Position(5, 8), new Position(5, 16)), hover.range());
    assertEquals("String", hover.type().dotPath());
    assertEquals("String", hover.type().presentable());
  }

  @Test
  @DisplayName("hover over nothing decodes to null")
  public void hoverOverNothingDecodesToNull() throws Exception {
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":null,"timestamp":1785368029.61}}""";
    assertNull(DisplayJson.decodeHover(DisplayJson.unwrap(payload)));
  }

  @Test
  @DisplayName("initialize decodes versions and the method list")
  public void initializeDecodesVersionsAndTheMethodList() throws Exception {
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":{
        "methods":["display/definition","display/diagnostics","server/invalidate"],
        "haxeVersion":{"major":4,"minor":3,"patch":7,"pre":null,"build":null},
        "protocolVersion":{"major":0,"minor":5,"patch":0}},"timestamp":1785368042.45}}""";

    InitializeResult result = DisplayJson.decodeInitialize(DisplayJson.unwrap(payload));

    assertEquals("4.3.7", result.haxeVersion().toString());
    assertEquals(0, result.protocolVersion().major());
    assertEquals(5, result.protocolVersion().minor());
    assertTrue(result.supports("display/diagnostics"));
    assertFalse(result.supports("display/hover"));
  }

  @Test
  @DisplayName("contexts decode signature platform and defines")
  public void contextsDecodeSignaturePlatformAndDefines() throws Exception {
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":[
        {"index":0,"desc":"after_init_macros","platform":"js",
         "classPaths":["./","C:\\\\haxe\\\\std/"],"signature":"c5da38c653f9",
         "defines":[{"key":"js","value":"1"},{"key":"utf16","value":"1"}]}],"timestamp":1.0}}""";

    List<HaxeServerContext> contexts = DisplayJson.decodeContexts(DisplayJson.unwrap(payload));

    assertEquals(1, contexts.size());
    HaxeServerContext context = contexts.get(0);
    assertEquals("after_init_macros", context.description());
    assertEquals("js", context.platform());
    assertEquals("c5da38c653f9", context.signature());
    assertEquals("1", context.defines().get("js"));
  }

  @Test
  @DisplayName("module types decode to qualified names")
  public void moduleTypesDecodeToQualifiedNames() throws Exception {
    // server/module of pack.Shapes, which declares its main type and a sub-type (trimmed)
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":{"id":7,"file":"src/pack/Shapes.hx","sign":"a1b2",
        "path":{"pack":["pack"],"moduleName":"Shapes"},
        "types":[
          {"pack":["pack"],"moduleName":"Shapes","typeName":"Shapes"},
          {"pack":["pack"],"moduleName":"Shapes","typeName":"Circle"}],
        "dependencies":[{"path":"haxe.ds.StringMap","sign":"c3d4"}],"dependents":[]},
        "timestamp":1.0}}""";

    ModuleInfo module = DisplayJson.decodeModule(DisplayJson.unwrap(payload));

    assertEquals("a1b2", module.sign());
    assertEquals(List.of("pack.Shapes", "pack.Shapes.Circle"), module.types());
    assertEquals(List.of("haxe.ds.StringMap"), module.dependencies());
  }

  @Test
  @DisplayName("type blueprint decodes fields and statics with member lookup")
  public void typeBlueprintDecodesFieldsAndStaticsWithMemberLookup() throws Exception {
    // captured server/type of a class with one field, one method, one static (trimmed)
    String payload = """
      {"jsonrpc":"2.0","id":1,"result":{"result":{"kind":"class","args":{
        "fields":[
          {"name":"label","type":{"kind":"TInst","args":{"path":{"pack":[],"moduleName":"String","typeName":"String"},"params":[]}}},
          {"name":"shout","type":{"kind":"TFun","args":{"args":[],
            "ret":{"kind":"TInst","args":{"path":{"pack":[],"moduleName":"String","typeName":"String"},"params":[]}}}}}],
        "statics":[
          {"name":"main","type":{"kind":"TFun","args":{"args":[],
            "ret":{"kind":"TAbstract","args":{"path":{"pack":[],"moduleName":"StdTypes","typeName":"Void"},"params":[]}}}}}]}},
        "timestamp":1.0}}""";

    JsonNode data = DisplayJson.unwrap(payload);
    TypeBlueprint blueprint = DisplayJson.decodeTypeBlueprint("Clean", data);

    assertEquals("class", blueprint.kind());
    assertEquals(2, blueprint.fields().size());
    assertEquals(1, blueprint.statics().size());

    TypeBlueprint.Member label = blueprint.findMember("label");
    assertNotNull(label);
    assertEquals("String", label.type().dotPath());

    TypeBlueprint.Member shout = blueprint.findMember("shout");
    assertNotNull(shout);
    assertTrue(shout.type().isFunction());
    assertEquals("() -> String", shout.type().presentable());

    TypeBlueprint.Member main = blueprint.findMember("main");
    assertNotNull(main);
    assertEquals("() -> Void", main.type().presentable());

    assertNull(blueprint.findMember("nope"));
  }
}
