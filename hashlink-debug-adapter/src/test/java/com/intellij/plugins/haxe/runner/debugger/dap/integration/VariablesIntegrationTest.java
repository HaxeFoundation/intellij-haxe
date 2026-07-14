package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.EvaluateRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.DebugErrorCode;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.ErrorResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.EvaluateResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.VariableKind;
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

    // Main.main takes no parameters, so these are classified plain locals (icon hint)
    List<Variable> raw = topFrameVariables(stopped.getBody().getThreadId());
    assertEquals("count is a local", VariableKind.LOCAL, findVariable(raw, "count").getKind());

    request(new DisconnectRequest());
  }

  @Test
  public void readsStackPassedArgumentsAfterStepIn() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    // step into add(total, i): its args are stack-passed on Windows x64
    assertTrue("stepIn accepted", request(stepInRequest(stopped.getBody().getThreadId())).isSuccess());
    int addThread = awaitStopped().getBody().getThreadId();
    Map<String, String> args = localsInTopFrame(addThread);
    assertEquals("current arg", "0", args.get("current"));
    assertEquals("amount arg", "0", args.get("amount"));

    // parameters carry the "argument" classification (drives the parameter icon)
    List<Variable> rawArgs = topFrameVariables(addThread);
    assertEquals("current is an argument", VariableKind.ARGUMENT, findVariable(rawArgs, "current").getKind());
    assertEquals("amount is an argument", VariableKind.ARGUMENT, findVariable(rawArgs, "amount").getKind());

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
    assertEquals("a frame local is classified local", VariableKind.LOCAL, p.getKind());

    Map<String, String> fields = variablesByName(p.getVariablesReference());
    assertEquals("Point.x", "10", fields.get("x"));
    assertEquals("Point.y", "20", fields.get("y"));
    assertEquals("Point.label", "\"origin\"", fields.get("label"));

    // an object's members carry the "field" classification (drives the field icon)
    assertEquals("Point.x is a field", VariableKind.FIELD, findVariable(variables(p.getVariablesReference()), "x").getKind());

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
    // ...but a function-TYPED static var (Null<Int->Void>, HFun at runtime) is
    // data, not a method binding, and must stay visible
    assertTrue("function-typed static var onBump shown", statics.containsKey("onBump"));

    // static fields carry the "static" classification (drives the static icon)
    assertEquals("Config.version is static", VariableKind.STATIC, findVariable(variables(staticsRef), "version").getKind());

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

  // --- scoping (shadowed names, dead bindings) ---

  @Test
  public void shadowingLoopVariableReplacesOuterInsideLoop() throws Exception {
    // `for (x in 0...n)` shadowing an outer String x: inside the loop there
    // must be exactly ONE x row, and it is the loop Int
    StoppedEvent stopped = runToBreakpoint(FIXTURE_SHADOW, FIXTURE_SHADOW_LOOP_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());
    assertEquals("exactly one x inside the loop", 1, countByName(locals, "x"));
    assertEquals("the loop Int shadows the outer String", "0", findVariable(locals, "x").getValue());

    request(new DisconnectRequest());
  }

  @Test
  public void shadowingLoopVariableGoesOutOfScopeAfterLoop() throws Exception {
    // after the loop the name must fall back to the outer String binding —
    // no ghost row tracking the recycled loop register
    StoppedEvent stopped = runToBreakpoint(FIXTURE_SHADOW, FIXTURE_SHADOW_AFTER_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());
    assertEquals("exactly one x after the loop", 1, countByName(locals, "x"));
    assertEquals("the outer String is back", "\"outer3\"", findVariable(locals, "x").getValue());
    assertEquals("total accumulated across the loop", "3", findVariable(locals, "total").getValue());

    request(new DisconnectRequest());
  }

  private static int countByName(List<Variable> variables, String name) {
    int count = 0;
    for (Variable variable : variables) {
      if (name.equals(variable.getName())) count++;
    }
    return count;
  }

  // --- registers scope ---

  @Test
  public void registersScopeListsCpuAndVmRegisters() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_SHADOW, FIXTURE_SHADOW_LOOP_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    Scope registers = scopeByPrefix(frameId, "Registers");
    assertNotNull("Registers scope present", registers);
    assertEquals("DAP presentation hint", "registers", registers.getPresentationHint());

    List<Variable> rows = variables(registers.getVariablesReference());
    // the architecture-neutral CPU subset leads the top frame's list
    Variable sp = findVariable(rows, "SP");
    assertNotNull("SP present", sp);
    assertTrue("SP is a hex pointer (was " + sp.getValue() + ")", sp.getValue().startsWith("0x"));
    assertNotNull("BP present", findVariable(rows, "BP"));
    assertNotNull("IP present", findVariable(rows, "IP"));
    assertNotNull("FLAGS present", findVariable(rows, "FLAGS"));

    // VM registers, annotated with the local currently bound to them
    Variable loopX = findByNameSuffix(rows, "(x)");
    assertNotNull("a VM register is annotated with the bound local x", loopX);
    assertEquals("the loop x register holds the iteration value", "0", loopX.getValue());
    assertNotNull("total's register annotated too", findByNameSuffix(rows, "(total)"));

    request(new DisconnectRequest());
  }

  @Test
  public void registersOnCallerFramesOmitCpuState() throws Exception {
    // CPU registers are thread state: shown on the top frame only, while every
    // frame lists its own VM registers
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_ADD_LINE);
    int callerFrameId = stackTrace(stopped.getBody().getThreadId()).getBody().getStackFrames().get(1).getId();
    Scope registers = scopeByPrefix(callerFrameId, "Registers");
    assertNotNull("caller frame has a Registers scope", registers);

    List<Variable> rows = variables(registers.getVariablesReference());
    assertEquals("no thread CPU rows on a caller frame", null, findVariable(rows, "SP"));
    assertNotNull("caller VM register bound to total", findByNameSuffix(rows, "(total)"));

    request(new DisconnectRequest());
  }

  private static Variable findByNameSuffix(List<Variable> variables, String suffix) {
    for (Variable variable : variables) {
      if (variable.getName() != null && variable.getName().endsWith(suffix)) return variable;
    }
    return null;
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
    // a trailing ';' (e.g. pasted from source) is ignored on a single-line expr
    assertEquals("trailing semicolon ignored", "2", evaluate(frameId, "n;").getBody().getResult());
    assertEquals("trailing semicolon + spaces ignored", "5", evaluate(frameId, "ints[1] ; ").getBody().getResult());

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
  public void evaluatesClassQualifiedStatics() throws Exception {
    // stop in Main (NOT Config / pkg.Deep): the class names must resolve from a
    // FOREIGN frame, which the locals → this → frame-statics order cannot
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // top-level class statics read from another class's frame
    assertEquals("Config.version", "7", evaluate(frameId, "Config.version").getBody().getResult());
    assertEquals("Config.title", "\"cfg\"", evaluate(frameId, "Config.title").getBody().getResult());
    // a PACKAGED class: the dotted class prefix spans path segments
    assertEquals("pkg.Deep.marker", "99", evaluate(frameId, "pkg.Deep.marker").getBody().getResult());
    // a function-typed static var (Null<Int->Void>) resolves by FQN — regression:
    // HFun statics were dropped as if they were methods, so this used to fail with
    // `"...Config" has no field "onBump"`
    assertTrue("Config.onBump resolves", evaluate(frameId, "Config.onBump").isSuccess());

    // writes resolve through the same prefix (restored right after: the
    // fixture's own output depends on Config.version)
    assertTrue("Config.version = 41", evaluate(frameId, "Config.version = 41").isSuccess());
    assertEquals("written static reads back", "41", evaluate(frameId, "Config.version").getBody().getResult());
    assertTrue("Config.version restored", evaluate(frameId, "Config.version = 7").isSuccess());

    // a bare class name evaluates to its expandable statics container
    EvaluateResponse cls = evaluate(frameId, "Config");
    assertTrue("class itself is expandable", cls.getBody().getVariablesReference() > 0);
    assertEquals("version listed under the class", "7",
                 variablesByName(cls.getBody().getVariablesReference()).get("version"));

    // unknown roots still fail clearly
    Response unknown = evaluateRaw(frameId, "NoSuchClass.value");
    assertFalse("unknown class root rejected", unknown.isSuccess());
    assertTrue("message names the root", unknown.getMessage().contains("NoSuchClass"));
    // machine-readable: the UnresolvedName code + the offending token, so a client
    // can resolve `NoSuchClass` against its own imports and re-issue qualified
    ErrorResponse err = (ErrorResponse)unknown;
    assertEquals("UnresolvedName code", DebugErrorCode.UNRESOLVED_NAME.id(), err.getBody().getError().getId());
    assertEquals("offending name in variables", "NoSuchClass", err.getBody().getError().getVariables().get("name"));

    request(new DisconnectRequest());
  }

  @Test
  public void evaluateRejectsBadExpressionsWithAClearMessage() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    Response unknown = evaluateRaw(frameId, "nosuch");
    assertFalse("unknown name must be rejected", unknown.isSuccess());
    assertTrue("message names the variable", unknown.getMessage().contains("nosuch"));

    Response unknownInExpr = evaluateRaw(frameId, "nosuch + 1");
    assertFalse("unknown name inside an expression must be rejected", unknownInExpr.isSuccess());
    assertTrue("message names the variable", unknownInExpr.getMessage().contains("nosuch"));

    Response malformed = evaluateRaw(frameId, "n +");
    assertFalse("malformed expression must be rejected", malformed.isSuccess());

    request(new DisconnectRequest());
  }

  // --- arbitrary expressions: operators folded adapter-side ---

  @Test
  public void evaluatesArithmeticAndLogicExpressions() throws Exception {
    // Mutate.demo checkpoint: n=5, flag=false, obj=Point(1,2,"p"), arr=[5,10,15], idx=1
    runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // arithmetic on locals, precedence, parentheses
    assertEquals("n + 1", "6", evaluate(frameId, "n + 1").getBody().getResult());
    assertEquals("n * 2 + 1", "11", evaluate(frameId, "n * 2 + 1").getBody().getResult());
    assertEquals("(n + 1) * 2", "12", evaluate(frameId, "(n + 1) * 2").getBody().getResult());
    assertEquals("division is Float (Haxe)", "2.5", evaluate(frameId, "n / 2").getBody().getResult());
    assertEquals("unary minus", "-5", evaluate(frameId, "-n").getBody().getResult());
    assertEquals("shift", "20", evaluate(frameId, "n << 2").getBody().getResult());

    // fields, statics and class-qualified statics as operands
    assertEquals("obj.x + obj.y", "3", evaluate(frameId, "obj.x + obj.y").getBody().getResult());
    // Config.bump() already ran by this point, so version is 8 (not the initial 7)
    assertEquals("Config.version + n", "13", evaluate(frameId, "Config.version + n").getBody().getResult());

    // array elements, including a COMPUTED index
    assertEquals("arr[1] + 1", "11", evaluate(frameId, "arr[1] + 1").getBody().getResult());
    assertEquals("computed index arr[idx]", "10", evaluate(frameId, "arr[idx]").getBody().getResult());
    assertEquals("computed index arr[idx + 1]", "15", evaluate(frameId, "arr[idx + 1]").getBody().getResult());

    // comparisons + logic (short-circuit)
    assertEquals("n > 4", "true", evaluate(frameId, "n > 4").getBody().getResult());
    assertEquals("n == 5 && !flag", "true", evaluate(frameId, "n == 5 && !flag").getBody().getResult());
    assertEquals("flag || n < 3", "false", evaluate(frameId, "flag || n < 3").getBody().getResult());
    assertEquals("string content compare", "true", evaluate(frameId, "obj.label == \"p\"").getBody().getResult());

    // string concat
    assertEquals("\"n=\" + n", "\"n=5\"", evaluate(frameId, "\"n=\" + n").getBody().getResult());
    assertEquals("label concat", "\"p!\"", evaluate(frameId, "obj.label + \"!\"").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void evaluatesTernaryAndTypeChecks() throws Exception {
    // Mutate.demo checkpoint: n=5, flag=false, obj=Point(1,2,"p"), arr=[5,10,15], idx=1
    runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // ternary — condition selects the branch, branches are full expressions
    assertEquals("n > 0 ? ... : ...", "\"pos\"", evaluate(frameId, "n > 0 ? \"pos\" : \"neg\"").getBody().getResult());
    assertEquals("false condition takes else", "2", evaluate(frameId, "flag ? 1 : 2").getBody().getResult());
    assertEquals("branch is an expression", "10", evaluate(frameId, "n > 3 ? n * 2 : 0").getBody().getResult());
    assertEquals("ternary right-assoc chain", "\"mid\"",
                 evaluate(frameId, "n < 0 ? \"lo\" : n > 100 ? \"hi\" : \"mid\"").getBody().getResult());
    // only the taken branch is evaluated: the untaken branch names an unknown
    // variable, which would ERROR if it ran
    assertEquals("untaken branch is not evaluated", "5", evaluate(frameId, "true ? n : nosuchvar").getBody().getResult());

    // `is` type checks
    assertEquals("object is its class", "true", evaluate(frameId, "obj is Point").getBody().getResult());
    assertEquals("object is not another class", "false", evaluate(frameId, "obj is String").getBody().getResult());
    assertEquals("int is Int", "true", evaluate(frameId, "n is Int").getBody().getResult());
    assertEquals("int is Float (Haxe)", "true", evaluate(frameId, "n is Float").getBody().getResult());
    assertEquals("int is not Bool", "false", evaluate(frameId, "n is Bool").getBody().getResult());
    assertEquals("field is String", "true", evaluate(frameId, "obj.label is String").getBody().getResult());
    // combined with logic
    assertEquals("is in a boolean expression", "true", evaluate(frameId, "obj is Point && n is Int").getBody().getResult());
    // an unknown type name is a user error, not a silent false
    Response unknownType = evaluateRaw(frameId, "obj is Nonexistent");
    assertFalse("unknown type rejected", unknownType.isSuccess());
    assertTrue("names the unknown type", unknownType.getMessage().contains("Nonexistent"));

    request(new DisconnectRequest());
  }

  @Test
  public void assignsExpressionResults() throws Exception {
    runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // expression RHS on a local
    assertTrue("n = n * 2 + 1", evaluate(frameId, "n = n * 2 + 1").isSuccess());
    assertEquals("n now 11", "11", evaluate(frameId, "n").getBody().getResult());

    // expression RHS on an array element with a COMPUTED index (idx=1)
    assertTrue("arr[idx] = n + 89", evaluate(frameId, "arr[idx] = n + 89").isSuccess());
    assertEquals("arr[1] now 100", "100", evaluate(frameId, "arr[1]").getBody().getResult());

    // boolean expression into a Bool local
    assertTrue("flag = n > 10", evaluate(frameId, "flag = n > 10").isSuccess());
    assertEquals("flag now true", "true", evaluate(frameId, "flag").getBody().getResult());

    request(new DisconnectRequest());
  }

  // --- container-element writes + instance method calls ---

  @Test
  public void writesArrayElementsThroughEvaluate() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // arr[i] = x directly (element address is writable) — ints is [2,5,10]
    assertTrue("ints[0] = 99", evaluate(frameId, "ints[0] = 99").isSuccess());
    assertEquals("ints[0] now 99", "99", evaluate(frameId, "ints[0]").getBody().getResult());
    // a path RHS into another element
    assertTrue("ints[2] = n", evaluate(frameId, "ints[2] = n").isSuccess());
    assertEquals("ints[2] now n (=2)", "2", evaluate(frameId, "ints[2]").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void callsInstanceMethodsAndMutatesAMap() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // read via a method call: stringMap has "a2"->2, "b"->6
    assertEquals("stringMap.get(\"b\")", "6", evaluate(frameId, "stringMap.get(\"b\")").getBody().getResult());
    // intMap has 2->"v2"; String values are pointers (no boxing needed)
    assertEquals("intMap.get(2)", "\"v2\"", evaluate(frameId, "intMap.get(2)").getBody().getResult());

    // MUTATE the map via its own method — the value-manipulation prize. A
    // String value is dynamic-compatible, so no boxing is required. The
    // insertion is proven by reading the new key back through get() (a missing
    // key returns null), the honest end-to-end signal.
    assertEquals("absent key is null before insert", "null", evaluate(frameId, "intMap.get(5)").getBody().getResult());
    assertTrue("intMap.set(5, \"hi\")", evaluate(frameId, "intMap.set(5, \"hi\")").isSuccess());
    assertEquals("the inserted entry reads back", "\"hi\"", evaluate(frameId, "intMap.get(5)").getBody().getResult());
    assertEquals("pre-existing entry intact", "\"v2\"", evaluate(frameId, "intMap.get(2)").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void mapBracketSyntaxSugarsToGetAndSet() throws Exception {
    // map[k] / map[k]=v are compile-time sugar for get/set; the evaluator
    // offers the same syntax by rewriting to the method calls.
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // read: stringMap["b"]==6 (String key), intMap[2]=="v2" (Int key)
    assertEquals("stringMap[\"b\"]", "6", evaluate(frameId, "stringMap[\"b\"]").getBody().getResult());
    assertEquals("intMap[2]", "\"v2\"", evaluate(frameId, "intMap[2]").getBody().getResult());
    assertEquals("absent key reads null", "null", evaluate(frameId, "stringMap[\"zz\"]").getBody().getResult());

    // write: string key with a boxed int value, and int key with a string value
    assertEquals("stringMap[\"c\"] = 9 returns the value", "9", evaluate(frameId, "stringMap[\"c\"] = 9").getBody().getResult());
    assertEquals("stringMap[\"c\"] reads back", "9", evaluate(frameId, "stringMap[\"c\"]").getBody().getResult());
    assertTrue("intMap[7] = \"seven\"", evaluate(frameId, "intMap[7] = \"seven\"").isSuccess());
    assertEquals("intMap[7] reads back", "\"seven\"", evaluate(frameId, "intMap[7]").getBody().getResult());

    // arrays are NOT maps: arr[i] stays a real indexed slot (read + write)
    assertEquals("ints[1] index read", "5", evaluate(frameId, "ints[1]").getBody().getResult());
    assertTrue("ints[1] = 42 index write", evaluate(frameId, "ints[1] = 42").isSuccess());
    assertEquals("ints[1] reads back the written index", "42", evaluate(frameId, "ints[1]").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void boxesPrimitivesIntoDynamicArguments() throws Exception {
    // stringMap is Map<String,Int>: values are stored BOXED (Dynamic). Passing
    // the int literal 9 requires boxing it into a vdynamic.
    // Proven end to end: set then read the value back.
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    assertEquals("absent before insert", "null", evaluate(frameId, "stringMap.get(\"c\")").getBody().getResult());
    assertTrue("stringMap.set(\"c\", 9) with boxing", evaluate(frameId, "stringMap.set(\"c\", 9)").isSuccess());
    assertEquals("boxed int reads back", "9", evaluate(frameId, "stringMap.get(\"c\")").getBody().getResult());
    // a pre-existing boxed value is unaffected
    assertEquals("existing entry intact", "6", evaluate(frameId, "stringMap.get(\"b\")").getBody().getResult());

    request(new DisconnectRequest());
  }

  @Test
  public void methodCallRejectsUnknownMethodsClearly() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    Response noSuch = evaluateRaw(frameId, "stringMap.nope(1)");
    assertFalse("unknown method rejected", noSuch.isSuccess());
    Response wrongArity = evaluateRaw(frameId, "stringMap.set(\"c\")");
    assertFalse("wrong arity rejected", wrongArity.isSuccess());
    assertTrue("arity message", wrongArity.getMessage().contains("argument"));

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
