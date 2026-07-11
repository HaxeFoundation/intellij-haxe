package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Reads variable values from a real HashLink debug session and asserts the KNOWN
 * fixture values — this validates the reconstructed frame offsets and value
 * layouts end to end. One value kind per test; each test stops at the fixture
 * line where that value is in scope.
 */
public class VariablesIntegrationTest extends DapIntegrationTestBase {

  // --- locals (frame offsets) ---

  @Test
  public void readsIntLocalsAtBreakpoint() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    // first iteration: i=0, count=3, total not yet updated on this line
    Map<String, String> locals = localsInTopFrame(stopped.getBody().getThreadId());
    assertEquals("count is 3", "3", locals.get("count"));
    assertEquals("i is 0 on first iteration", "0", locals.get("i"));
    assertEquals("total is 0 before first add", "0", locals.get("total"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsStackPassedArgumentsAfterStepIn() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    // step into add(total, i): its args are stack-passed on Windows x64
    assertTrue("stepIn accepted", request(stepInRequest(stopped.getBody().getThreadId())).isSuccess());
    Map<String, String> args = localsInTopFrame(awaitStopped().getBody().getThreadId());
    assertEquals("current arg", "0", args.get("current"));
    assertEquals("amount arg", "0", args.get("amount"));

    request(new DisconnectRequest());
  }

  @Test
  public void tracksLocalValuesAcrossLoopIterations() throws Exception {
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    Map<String, String> secondStop = localsInTopFrame(continueToNextStop().getBody().getThreadId());
    assertEquals("i is 1 on second iteration", "1", secondStop.get("i"));

    Map<String, String> thirdStop = localsInTopFrame(continueToNextStop().getBody().getThreadId());
    assertEquals("i is 2 on third iteration", "2", thirdStop.get("i"));
    assertEquals("total is 1 after two adds", "1", thirdStop.get("total"));

    request(new DisconnectRequest());
  }

  // --- objects ---

  @Test
  public void expandsObjectFields() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);

    Variable p = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "p");
    assertNotNull("local p present", p);
    assertTrue("Point is expandable", p.getVariablesReference() > 0);
    assertEquals("p typed as Point", "Point", p.getType());

    Map<String, String> fields = variablesByName(p.getVariablesReference());
    assertEquals("Point.x", "10", fields.get("x"));
    assertEquals("Point.y", "20", fields.get("y"));
    assertEquals("Point.label", "\"origin\"", fields.get("label"));

    request(new DisconnectRequest());
  }

  // --- statics ---

  @Test
  public void readsStaticFields() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);

    // the frame is Config.bump — its Statics scope holds version=7, title="cfg"
    int staticsRef = staticsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    Map<String, String> statics = variablesByName(staticsRef);
    assertEquals("Config.version", "7", statics.get("version"));
    assertEquals("Config.title", "\"cfg\"", statics.get("title"));
    // the static method sharing the container must not leak into the scope
    assertFalse("bump() hidden from Statics", statics.containsKey("bump"));

    request(new DisconnectRequest());
  }

  // --- rich values, one kind per test (all in scope at FIXTURE_RICH_LINE) ---

  @Test
  public void readsIntArrayElements() throws Exception {
    // Array<Int> -> hl.types.ArrayBytes_Int: elements straight from the bytes
    Variable ints = findVariable(richLocals(), "ints");
    assertNotNull("local ints present", ints);
    assertEquals("ints preview", "Array(3)", ints.getValue());
    Map<String, String> elements = variablesByName(ints.getVariablesReference());
    assertEquals("ints[0]", "2", elements.get("0"));
    assertEquals("ints[1]", "5", elements.get("1"));
    assertEquals("ints[2]", "10", elements.get("2"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsStringArrayElements() throws Exception {
    // Array<String> -> hl.types.ArrayObj: elements typed via the varray's runtime type
    Variable names = findVariable(richLocals(), "names");
    assertNotNull("local names present", names);
    assertEquals("names preview", "Array(2)", names.getValue());
    Map<String, String> elements = variablesByName(names.getVariablesReference());
    assertEquals("names[0]", "\"a2\"", elements.get("0"));
    assertEquals("names[1]", "\"b\"", elements.get("1"));

    request(new DisconnectRequest());
  }

  @Test
  public void unboxesDynamicHoldingAnInt() throws Exception {
    // vdynamic: runtime type @ +0, payload @ +8
    Variable dyn = findVariable(richLocals(), "dyn");
    assertNotNull("local dyn present", dyn);
    assertEquals("dyn unboxes via runtime type", "42", dyn.getValue());

    request(new DisconnectRequest());
  }

  @Test
  public void readsEnumConstructorAndParams() throws Exception {
    Variable shade = findVariable(richLocals(), "shade");
    assertNotNull("local shade present", shade);
    assertEquals("enum inline preview", "Tinted(2, \"red\")", shade.getValue());
    Map<String, String> params = variablesByName(shade.getVariablesReference());
    assertEquals("Tinted param 0", "2", params.get("0"));
    assertEquals("Tinted param 1", "\"red\"", params.get("1"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsAnonymousStructureFields() throws Exception {
    // anonymous structure (virtual): fields via the indirect pointers
    Variable anon = findVariable(richLocals(), "anon");
    assertNotNull("local anon present", anon);
    assertTrue("anon is expandable", anon.getVariablesReference() > 0);
    Map<String, String> fields = variablesByName(anon.getVariablesReference());
    assertEquals("anon.width", "2", fields.get("width"));
    assertEquals("anon.tag", "\"t2\"", fields.get("tag"));

    request(new DisconnectRequest());
  }

  @Test
  public void rendersClosureAsFunction() throws Exception {
    Variable f = findVariable(richLocals(), "f");
    assertNotNull("local f present", f);
    assertTrue("closure renders as a function (was " + f.getValue() + ")",
               f.getValue().startsWith("function"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsCompilerBoxedCapturedLocal() throws Exception {
    // a local mutated by a closure is boxed by genhl into a 1-element array;
    // it must stay inspectable (expand to the current value), not render raw
    Variable captured = findVariable(richLocals(), "captured");
    assertNotNull("local captured present", captured);
    assertEquals("capture box preview", "Array(1)", captured.getValue());
    Map<String, String> box = variablesByName(captured.getVariablesReference());
    assertEquals("boxed value", "20", box.get("0"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsRefLocalThroughIndirection() throws Exception {
    // hl.Ref.make(n) yields an HRef(i32) local: the value must read through
    // the indirection, not render as a raw pointer
    Variable byRef = findVariable(richLocals(), "byRef");
    assertNotNull("local byRef present", byRef);
    assertEquals("ref-typed local reads its target", "2", byRef.getValue());

    request(new DisconnectRequest());
  }

  @Test
  public void readsDynamicArrayElements() throws Exception {
    // Array<Dynamic> -> hl.types.ArrayDyn: elements via the wrapped ArrayBase
    Variable dynArray = findVariable(richLocals(), "dynArray");
    assertNotNull("local dynArray present", dynArray);
    assertEquals("dynArray preview", "Array(2)", dynArray.getValue());
    Map<String, String> elements = variablesByName(dynArray.getVariablesReference());
    assertEquals("dynArray[0] unboxes an Int", "2", elements.get("0"));
    assertEquals("dynArray[1] is a String", "\"s2\"", elements.get("1"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsDynamicObjectFields() throws Exception {
    // a Dynamic with dynamic field writes is a runtime dynobj: fields are
    // resolved through the hashed lookup table (typedef/anon-through-Dynamic case)
    Variable dynObj = findVariable(richLocals(), "dynObj");
    assertNotNull("local dynObj present", dynObj);
    assertTrue("dynObj is expandable (was " + dynObj.getValue() + ")", dynObj.getVariablesReference() > 0);
    Map<String, String> fields = variablesByName(dynObj.getVariablesReference());
    assertEquals("dynObj.score", "2", fields.get("score"));
    assertEquals("dynObj.label", "\"d2\"", fields.get("label"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsStringMapEntries() throws Exception {
    Variable map = findVariable(richLocals(), "stringMap");
    assertNotNull("local stringMap present", map);
    assertEquals("stringMap preview", "Map(2)", map.getValue());
    Map<String, String> entries = variablesByName(map.getVariablesReference());
    assertEquals("stringMap[a2]", "2", entries.get("\"a2\""));
    assertEquals("stringMap[b]", "6", entries.get("\"b\""));

    request(new DisconnectRequest());
  }

  @Test
  public void readsIntMapEntries() throws Exception {
    Variable map = findVariable(richLocals(), "intMap");
    assertNotNull("local intMap present", map);
    assertEquals("intMap preview", "Map(1)", map.getValue());
    Map<String, String> entries = variablesByName(map.getVariablesReference());
    assertEquals("intMap[2]", "\"v2\"", entries.get("2"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsEnumValueMapEntries() throws Exception {
    // haxe.ds.EnumValueMap is a pure-Haxe balanced tree, walked in order
    Variable map = findVariable(richLocals(), "enumMap");
    assertNotNull("local enumMap present", map);
    assertEquals("enumMap preview", "Map(2)", map.getValue());
    Map<String, String> entries = variablesByName(map.getVariablesReference());
    assertEquals("enumMap[Plain]", "2", entries.get("Plain"));
    assertEquals("enumMap[Tinted(2, \"x\")]", "4", entries.get("Tinted(2, \"x\")"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsBareNativeMapAbstract() throws Exception {
    // a raw hl_bytes_map abstract (StringMap internals, no wrapper): the
    // abstract pointer IS the native map and lists its entries directly
    Variable nativeMap = findVariable(richLocals(), "nativeMap");
    assertNotNull("local nativeMap present", nativeMap);
    assertEquals("nativeMap preview", "Map(2)", nativeMap.getValue());
    Map<String, String> entries = variablesByName(nativeMap.getVariablesReference());
    assertEquals("nativeMap[a2]", "2", entries.get("\"a2\""));
    assertEquals("nativeMap[b]", "6", entries.get("\"b\""));

    request(new DisconnectRequest());
  }

  @Test
  public void resolvesAbstractNameThroughDynamic() throws Exception {
    // a Dynamic holding an abstract: the runtime HABSTRACT kind resolves the
    // abstract's name, so the map decodes instead of showing "Dynamic @ 0x…"
    Variable dynAbstract = findVariable(richLocals(), "dynAbstract");
    assertNotNull("local dynAbstract present", dynAbstract);
    assertEquals("dynAbstract preview", "Map(2)", dynAbstract.getValue());

    request(new DisconnectRequest());
  }

  @Test
  public void readsPackedStructField() throws Exception {
    // @:packed field: the Vec2 struct is inlined into the PackedHolder
    // instance — wrong packed offsets would corrupt id/tail too
    Variable holder = findVariable(richLocals(), "holder");
    assertNotNull("local holder present", holder);
    Map<String, String> fields = variablesByName(holder.getVariablesReference());
    assertEquals("holder.id", "2", fields.get("id"));
    assertEquals("holder.tail", "4", fields.get("tail"));

    Variable pos = findVariable(variables(holder.getVariablesReference()), "pos");
    assertNotNull("packed field present", pos);
    assertEquals("packed field typed as the struct", "Vec2", pos.getType());
    assertTrue("packed field is expandable", pos.getVariablesReference() > 0);
    Map<String, String> vec = variablesByName(pos.getVariablesReference());
    assertEquals("holder.pos.x", "1.5", vec.get("x"));
    assertEquals("holder.pos.y", "2.5", vec.get("y"));

    request(new DisconnectRequest());
  }

  @Test
  public void readsStructLocal() throws Exception {
    // a @:struct class local (HStruct): fields at base 0, no hl_type* header
    Variable vec = findVariable(richLocals(), "vec");
    assertNotNull("local vec present", vec);
    assertTrue("struct is expandable", vec.getVariablesReference() > 0);
    Map<String, String> fields = variablesByName(vec.getVariablesReference());
    assertEquals("vec.x", "3.25", fields.get("x"));
    assertEquals("vec.y", "7", fields.get("y"));

    request(new DisconnectRequest());
  }

  @Test
  public void expandsClosureCapturedValue() throws Exception {
    // a bound closure (hasValue == 1): the capture environment is a child;
    // here f captures one mutated local, boxed by genhl into a 1-element array
    Variable f = findVariable(richLocals(), "f");
    assertNotNull("local f present", f);
    assertTrue("bound closure is expandable", f.getVariablesReference() > 0);
    Variable captured = findVariable(variables(f.getVariablesReference()), "captured");
    assertNotNull("captured child present", captured);
    assertEquals("capture box preview", "Array(1)", captured.getValue());
    Map<String, String> box = variablesByName(captured.getVariablesReference());
    assertEquals("captured value inside the box", "20", box.get("0"));

    request(new DisconnectRequest());
  }

  // --- evaluate (variable paths) ---

  @Test
  public void evaluatesLocalAndPaths() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    assertEquals("plain local", "2", evaluate(frameId, "n").getBody().getResult());
    assertEquals("array index", "5", evaluate(frameId, "ints[1]").getBody().getResult());
    assertEquals("dynobj field path", "\"d2\"", evaluate(frameId, "dynObj.label").getBody().getResult());
    assertEquals("packed struct path", "1.5", evaluate(frameId, "holder.pos.x").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void evaluatesThisFieldAndStatics() throws Exception {
    runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // implicit this.field inside Point.move
    assertEquals("implicit this field", "10", evaluate(frameId, "x").getBody().getResult());
    assertEquals("explicit this path", "20", evaluate(frameId, "this.y").getBody().getResult());
    // static of the owning class
    assertEquals("class static", "2", evaluate(frameId, "axes").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void evaluateRejectsExpressionsWithAClearMessage() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    Response rejected = evaluateRaw(frameId, "n + 1");
    assertFalse("arithmetic must be rejected", rejected.isSuccess());
    assertTrue("message names the limitation (was: " + rejected.getMessage() + ")",
               rejected.getMessage().contains("variable paths"));

    Response unknown = evaluateRaw(frameId, "nosuch");
    assertFalse("unknown name must be rejected", unknown.isSuccess());
    assertTrue("message names the variable", unknown.getMessage().contains("nosuch"));

    request(new DisconnectRequest());
  }

  private EvaluateResponse evaluate(int frameId, String expression) throws Exception {
    Response response = evaluateRaw(frameId, expression);
    assertTrue("evaluate '" + expression + "' succeeds: " + response.getMessage(), response.isSuccess());
    return (EvaluateResponse)response;
  }

  private Response evaluateRaw(int frameId, String expression) throws Exception {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setExpression(expression);
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request(request);
  }

  // --- instance methods ---

  @Test
  public void staticsScopeAppearsInInstanceMethods() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);

    // stopped inside Point.move (an instance method): Point's statics must show
    int staticsRef = staticsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    Map<String, String> statics = variablesByName(staticsRef);
    assertEquals("Point.axes", "2", statics.get("axes"));
    // compiler bookkeeping like __name__ must be hidden
    for (String name : statics.keySet()) {
      assertFalse("compiler field leaked into Statics: " + name,
                  name.startsWith("__") && name.endsWith("__"));
    }

    request(new DisconnectRequest());
  }

  @Test
  public void showsThisInInstanceMethod() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);

    // stopped inside Point.move: `this` must be listed and expand to the Point
    Variable self = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "this");
    assertNotNull("`this` present in an instance-method frame", self);
    assertTrue("`this` is expandable", self.getVariablesReference() > 0);
    Map<String, String> fields = variablesByName(self.getVariablesReference());
    assertEquals("this.x (move not applied yet)", "10", fields.get("x"));
    assertEquals("this.y", "20", fields.get("y"));

    request(new DisconnectRequest());
  }

  /** Stops at FIXTURE_RICH_LINE and returns Rich.demo's locals. */
  private List<Variable> richLocals() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    return topFrameVariables(stopped.getBody().getThreadId());
  }
}
