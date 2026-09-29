package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Scope;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Variable;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.VariableKind;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Reads variable values from a real HashLink debug session and asserts the KNOWN
 * fixture values — this validates the reconstructed frame offsets and value
 * layouts end to end. One value kind per test; each test stops at the fixture
 * line where that value is in scope.
 */
@DisplayName("HashLink debugger: variables (integration)")
public class VariablesIntegrationTest extends DapIntegrationTestBase {
  // --- locals (frame offsets) ---

  @Test
  @DisplayName("reads int locals at breakpoint")
  public void readsIntLocalsAtBreakpoint() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    // first iteration: i=0, count=3, total not yet updated on this line
    Map<String, String> locals = localsInTopFrame(stopped.getBody().getThreadId());
    assertEquals("3", locals.get("count"), "count is 3");
    assertEquals("0", locals.get("i"), "i is 0 on first iteration");
    assertEquals("0", locals.get("total"), "total is 0 before first add");

    // Main.main takes no parameters, so these are classified plain locals (icon hint)
    List<Variable> raw = topFrameVariables(stopped.getBody().getThreadId());
    assertEquals(VariableKind.LOCAL, findVariable(raw, "count").getKind(), "count is a local");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("reads stack passed arguments after step in")
  public void readsStackPassedArgumentsAfterStepIn() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    // step into add(total, i): its args are stack-passed on Windows x64
    assertTrue(request(stepInRequest(stopped.getBody().getThreadId())).isSuccess(), "stepIn accepted");
    int addThread = awaitStopped().getBody().getThreadId();
    Map<String, String> args = localsInTopFrame(addThread);
    assertEquals("0", args.get("current"), "current arg");
    assertEquals("0", args.get("amount"), "amount arg");

    // parameters carry the "argument" classification (drives the parameter icon)
    List<Variable> rawArgs = topFrameVariables(addThread);
    assertEquals(VariableKind.ARGUMENT, findVariable(rawArgs, "current").getKind(), "current is an argument");
    assertEquals(VariableKind.ARGUMENT, findVariable(rawArgs, "amount").getKind(), "amount is an argument");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("tracks local values across loop iterations")
  public void tracksLocalValuesAcrossLoopIterations() throws Exception {
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    Map<String, String> secondStop = localsInTopFrame(continueToNextStop().getBody().getThreadId());
    assertEquals("1", secondStop.get("i"), "i is 1 on second iteration");

    Map<String, String> thirdStop = localsInTopFrame(continueToNextStop().getBody().getThreadId());
    assertEquals("2", thirdStop.get("i"), "i is 2 on third iteration");
    assertEquals("1", thirdStop.get("total"), "total is 1 after two adds");

    request(new DisconnectRequest());
  }

  // --- objects ---

  @Test
  @DisplayName("expands object fields")
  public void expandsObjectFields() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);
    Variable p = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "p");
    assertNotNull(p, "local p present");
    assertTrue(p.getVariablesReference() > 0, "Point is expandable");
    assertEquals("Point", p.getType(), "p typed as Point");
    assertEquals(VariableKind.LOCAL, p.getKind(), "a frame local is classified local");

    Map<String, String> fields = variablesByName(p.getVariablesReference());
    assertEquals("10", fields.get("x"), "Point.x");
    assertEquals("20", fields.get("y"), "Point.y");
    assertEquals("\"origin\"", fields.get("label"), "Point.label");

    // an object's members carry the "field" classification (drives the field icon)
    assertEquals(VariableKind.FIELD, findVariable(variables(p.getVariablesReference()), "x").getKind(), "Point.x is a field");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("closure array elements render as functions")
  public void closureArrayElementsRenderAsFunctions() throws Exception {
    // An Array<()->Int> element reaches its value through a DYNAMIC slot: the
    // runtime fun-type header must resolve (RuntimeTypes KFUN) so the element
    // renders like a statically typed closure variable — name and signature —
    // instead of a bare "Dynamic" (user-reported).
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CLOSURE, FIXTURE_CLOSURE_REAL_ARRAY_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());
    Variable callbacks = findVariable(locals, "callbacks");
    assertNotNull(callbacks, "local callbacks present in " + locals);
    assertTrue(callbacks.getVariablesReference() > 0, "the array expands");

    Variable element = findVariable(variables(callbacks.getVariablesReference()), "0");
    assertNotNull(element, "element 0 present");
    String rendered = element.getValue();
    assertTrue(rendered.startsWith("function ") && rendered.contains("grab"), "the element names its function (was " + rendered + ")");
    assertEquals("() -> Int", element.getType(), "with the reconstructed signature as its type");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("to string rendering toggle is accepted and inert for now")
  public void toStringRenderingToggleIsAcceptedAndInertForNow() throws Exception {
    // The custom custom/setToStringRendering request is part of the wire
    // contract (the IDE's live gear toggle sends it), but the adapter only
    // STORES the flag: labels must stay class names until the fault-proof
    // (hl_dyn_call_safe) rendering lands — a plain injected toString that
    // faults is unrecoverable, the debug API cannot continue past it.
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);

    assertTrue(request(SetToStringRenderingRequest.of(true)).isSuccess(), "toggle on accepted");
    Variable p = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "p");
    assertEquals("Point", p.getValue(), "labels unchanged until the safe rendering lands");
    assertTrue(request(SetToStringRenderingRequest.of(false)).isSuccess(), "toggle off accepted");

    request(new DisconnectRequest());
  }

  // --- statics ---

  @Test
  @DisplayName("reads static fields")
  public void readsStaticFields() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_CONFIG, FIXTURE_STATICS_LINE);

    // the frame is Config.bump — its Statics scope holds version=7, title="cfg"
    int staticsRef = staticsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    Map<String, String> statics = variablesByName(staticsRef);
    assertEquals("7", statics.get("version"), "Config.version");
    assertEquals("\"cfg\"", statics.get("title"), "Config.title");
    // the static method sharing the container must not leak into the scope
    assertFalse(statics.containsKey("bump"), "bump() hidden from Statics");
    // ...but a function-TYPED static var (Null<Int->Void>, HFun at runtime) is
    // data, not a method binding, and must stay visible
    assertTrue(statics.containsKey("onBump"), "function-typed static var onBump shown");

    // static fields carry the "static" classification (drives the static icon)
    assertEquals(VariableKind.STATIC, findVariable(variables(staticsRef), "version").getKind(), "Config.version is static");

    request(new DisconnectRequest());
  }

  // --- rich values (all in scope at FIXTURE_RICH_LINE) ---

  /**
   * Container-shaped locals: rendered preview (null = only assert expandable)
   * and the children read through the variables reference. One runtime layout
   * per row; the layout fact sits on its row.
   */
  static Stream<Arguments> containerLocals() {
    return Stream.of(
      // Array<Int> -> hl.types.ArrayBytes_Int: elements straight from the bytes
      arguments("ints", "Array(3)", Map.of("0", "2", "1", "5", "2", "10")),
      // Array<String> -> hl.types.ArrayObj: elements typed via the varray's runtime type
      arguments("names", "Array(2)", Map.of("0", "\"a2\"", "1", "\"b\"")),
      // an enum value: constructor preview, params as indexed children
      arguments("shade", "Tinted(2, \"red\")", Map.of("0", "2", "1", "\"red\"")),
      // anonymous structure (virtual): fields via the indirect pointers
      arguments("anon", null, Map.of("width", "2", "tag", "\"t2\"")),
      // a local mutated by a closure is boxed by genhl into a 1-element array;
      // it must stay inspectable (expand to the current value), not render raw
      arguments("captured", "Array(1)", Map.of("0", "20")),
      // Array<Dynamic> -> hl.types.ArrayDyn: elements via the wrapped ArrayBase
      arguments("dynArray", "Array(2)", Map.of("0", "2", "1", "\"s2\"")),
      // a Dynamic with dynamic field writes is a runtime dynobj: fields are
      // resolved through the hashed lookup table (typedef/anon-through-Dynamic case)
      arguments("dynObj", null, Map.of("score", "2", "label", "\"d2\"")),
      arguments("stringMap", "Map(2)", Map.of("\"a2\"", "2", "\"b\"", "6")),
      arguments("intMap", "Map(1)", Map.of("2", "\"v2\"")),
      // haxe.ds.EnumValueMap is a pure-Haxe balanced tree, walked in order
      arguments("enumMap", "Map(2)", Map.of("Plain", "2", "Tinted(2, \"x\")", "4")),
      // a raw hl_bytes_map abstract (StringMap internals, no wrapper): the
      // abstract pointer IS the native map and lists its entries directly
      arguments("nativeMap", "Map(2)", Map.of("\"a2\"", "2", "\"b\"", "6")),
      // a @:struct class local (HStruct): fields at base 0, no hl_type* header
      arguments("vec", null, Map.of("x", "3.25", "y", "7")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("containerLocals")
  @DisplayName("reads container locals")
  public void readsContainerLocals(String name, String preview, Map<String, String> children) throws Exception {
    Variable local = findVariable(richLocals(), name);
    assertNotNull(local, "local " + name + " present");
    assertTrue(local.getVariablesReference() > 0, name + " is expandable (was " + local.getValue() + ")");
    if (preview != null) {
      assertEquals(preview, local.getValue(), name + " preview");
    }
    Map<String, String> actual = variablesByName(local.getVariablesReference());
    children.forEach((child, value) -> assertEquals(value, actual.get(child), name + "[" + child + "]"));

    request(new DisconnectRequest());
  }

  /** Leaf locals whose rendered value alone is the pin. */
  static Stream<Arguments> leafLocals() {
    return Stream.of(
      // vdynamic: runtime type @ +0, payload @ +8 - unboxes via the runtime type
      arguments("dyn", "42"),
      // hl.Ref.make(n) yields an HRef(i32) local: the value reads through the
      // indirection, not as a raw pointer
      arguments("byRef", "2"),
      // a Dynamic holding an abstract: the runtime HABSTRACT kind resolves the
      // abstract's name, so the map decodes instead of showing "Dynamic @ 0x…"
      arguments("dynAbstract", "Map(2)"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("leafLocals")
  @DisplayName("reads leaf locals")
  public void readsLeafLocals(String name, String value) throws Exception {
    Variable local = findVariable(richLocals(), name);
    assertNotNull(local, "local " + name + " present");
    assertEquals(value, local.getValue(), name + " value");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("renders closure as function")
  public void rendersClosureAsFunction() throws Exception {
    Variable f = findVariable(richLocals(), "f");
    assertNotNull(f, "local f present");
    assertTrue(f.getValue().startsWith("function"), "closure renders as a function (was " + f.getValue() + ")");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("reads packed struct field")
  public void readsPackedStructField() throws Exception {
    // @:packed field: the Vec2 struct is inlined into the PackedHolder
    // instance — wrong packed offsets would corrupt id/tail too
    Variable holder = findVariable(richLocals(), "holder");
    assertNotNull(holder, "local holder present");
    Map<String, String> fields = variablesByName(holder.getVariablesReference());
    assertEquals("2", fields.get("id"), "holder.id");
    assertEquals("4", fields.get("tail"), "holder.tail");

    Variable pos = findVariable(variables(holder.getVariablesReference()), "pos");
    assertNotNull(pos, "packed field present");
    assertEquals("Vec2", pos.getType(), "packed field typed as the struct");
    assertTrue(pos.getVariablesReference() > 0, "packed field is expandable");
    Map<String, String> vec = variablesByName(pos.getVariablesReference());
    assertEquals("1.5", vec.get("x"), "holder.pos.x");
    assertEquals("2.5", vec.get("y"), "holder.pos.y");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("expands closure captured value")
  public void expandsClosureCapturedValue() throws Exception {
    // a bound closure (hasValue == 1): the capture environment is a child;
    // the box's CONTENT is the "captured" row of the container-locals table
    Variable f = findVariable(richLocals(), "f");
    assertNotNull(f, "local f present");
    assertTrue(f.getVariablesReference() > 0, "bound closure is expandable");

    Variable captured = findVariable(variables(f.getVariablesReference()), "captured");
    assertNotNull(captured, "captured child present");

    request(new DisconnectRequest());
  }

  // --- scoping (shadowed names, dead bindings) ---

  @Test
  @DisplayName("shadowing loop variable replaces outer inside loop")
  public void shadowingLoopVariableReplacesOuterInsideLoop() throws Exception {
    // `for (x in 0...n)` shadowing an outer String x: inside the loop there
    // must be exactly ONE x row, and it is the loop Int
    StoppedEvent stopped = runToBreakpoint(FIXTURE_SHADOW, FIXTURE_SHADOW_LOOP_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());
    assertEquals(1, countByName(locals, "x"), "exactly one x inside the loop");
    assertEquals("0", findVariable(locals, "x").getValue(), "the loop Int shadows the outer String");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("shadowing loop variable goes out of scope after loop")
  public void shadowingLoopVariableGoesOutOfScopeAfterLoop() throws Exception {
    // after the loop the name must fall back to the outer String binding —
    // no ghost row tracking the recycled loop register
    StoppedEvent stopped = runToBreakpoint(FIXTURE_SHADOW, FIXTURE_SHADOW_AFTER_LINE);
    List<Variable> locals = topFrameVariables(stopped.getBody().getThreadId());
    assertEquals(1, countByName(locals, "x"), "exactly one x after the loop");
    assertEquals("\"outer3\"", findVariable(locals, "x").getValue(), "the outer String is back");
    assertEquals("3", findVariable(locals, "total").getValue(), "total accumulated across the loop");

    request(new DisconnectRequest());
  }

  // --- registers scope ---

  @Test
  @DisplayName("registers scope lists cpu and vm registers")
  public void registersScopeListsCpuAndVmRegisters() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_SHADOW, FIXTURE_SHADOW_LOOP_LINE);
    int frameId = topFrameId(stopped.getBody().getThreadId());
    Scope registers = scopeByPrefix(frameId, "Registers");
    assertNotNull(registers, "Registers scope present");
    assertEquals("registers", registers.getPresentationHint(), "DAP presentation hint");

    List<Variable> rows = variables(registers.getVariablesReference());
    // the architecture-neutral CPU subset leads the top frame's list
    Variable sp = findVariable(rows, "SP");
    assertNotNull(sp, "SP present");
    assertTrue(sp.getValue().startsWith("0x"), "SP is a hex pointer (was " + sp.getValue() + ")");
    assertNotNull(findVariable(rows, "BP"), "BP present");
    assertNotNull(findVariable(rows, "IP"), "IP present");
    assertNotNull(findVariable(rows, "FLAGS"), "FLAGS present");

    // VM registers, annotated with the local currently bound to them
    Variable loopX = findByNameSuffix(rows, "(x)");
    assertNotNull(loopX, "a VM register is annotated with the bound local x");
    assertEquals("0", loopX.getValue(), "the loop x register holds the iteration value");
    assertNotNull(findByNameSuffix(rows, "(total)"), "total's register annotated too");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("registers on caller frames omit cpu state")
  public void registersOnCallerFramesOmitCpuState() throws Exception {
    // CPU registers are thread state: shown on the top frame only, while every
    // frame lists its own VM registers
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_ADD_LINE);
    int callerFrameId = stackTrace(stopped.getBody().getThreadId()).getBody().getStackFrames().get(1).getId();
    Scope registers = scopeByPrefix(callerFrameId, "Registers");
    assertNotNull(registers, "caller frame has a Registers scope");

    List<Variable> rows = variables(registers.getVariablesReference());
    assertEquals(null, findVariable(rows, "SP"), "no thread CPU rows on a caller frame");
    assertNotNull(findByNameSuffix(rows, "(total)"), "caller VM register bound to total");

    request(new DisconnectRequest());
  }

  // --- evaluate (variable paths) ---

  @Test
  @DisplayName("evaluates local and paths")
  public void evaluatesLocalAndPaths() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    assertEquals("2", evaluated(frameId, "n"), "plain local");
    assertEquals("5", evaluated(frameId, "ints[1]"), "array index");
    assertEquals("\"d2\"", evaluated(frameId, "dynObj.label"), "dynobj field path");
    assertEquals("1.5", evaluated(frameId, "holder.pos.x"), "packed struct path");
    // a trailing ';' (e.g. pasted from source) is ignored on a single-line expr
    assertEquals("2", evaluated(frameId, "n;"), "trailing semicolon ignored");
    assertEquals("5", evaluated(frameId, "ints[1] ; "), "trailing semicolon + spaces ignored");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("evaluates leaf member paths")
  public void evaluatesLeafMemberPaths() throws Exception {
    // The Variables view renders a String as a childless leaf (its content,
    // not bytes/length), so the direct reference walk cannot descend into it;
    // these paths must fall back to the typed interpreter, which reads the
    // REAL HObj fields (String.length and ArrayBase.length are physical I32s).
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    assertEquals("2", evaluated(frameId, "names[0].length"), "string element length");
    assertEquals("3", evaluated(frameId, "ints.length"), "array length (bytes-backed)");
    assertEquals("2", evaluated(frameId, "names.length"), "array length (object-backed)");
    assertEquals("2", evaluated(frameId, "dynArray.length"), "array length (dynamic)");
    // a push-built Array<String> (not a literal) — the everyday shape
    assertEquals("2", evaluated(frameId, "pushed.length"), "pushed array length");
    assertEquals("2", evaluated(frameId, "pushed[0].length"), "pushed string element length");
    // a String element behind a DYNAMIC slot: the static element type says
    // nothing, the value's own header does — dynArray[1] is "s2"
    assertEquals("2", evaluated(frameId, "dynArray[1].length"), "dynamic-slot string length");

    // a genuinely missing member still fails, with the walk's message
    Response missing = evaluateRaw(frameId, "names[0].nope");
    assertFalse(missing.isSuccess(), "bogus member rejected");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("evaluates string local length")
  public void evaluatesStringLocalLength() throws Exception {
    // the reported case: `s.length` on a plain String local (s = "orig10")
    runToBreakpoint(FIXTURE_CALL, FIXTURE_CALL_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    assertEquals("6", evaluated(frameId, "s.length"), "string local length");
    assertEquals("7", evaluated(frameId, "s.length + 1"), "length inside an expression");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("evaluates this field and statics")
  public void evaluatesThisFieldAndStatics() throws Exception {
    runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // implicit this.field inside Point.move
    assertEquals("10", evaluated(frameId, "x"), "implicit this field");
    assertEquals("20", evaluated(frameId, "this.y"), "explicit this path");
    // static of the owning class
    assertEquals("2", evaluated(frameId, "axes"), "class static");

    request(new DisconnectRequest());
  }

  // Class-qualified statics stop in Main (NOT Config / pkg.Deep): the class
  // names must resolve from a FOREIGN frame, which the locals → this →
  // frame-statics order cannot.

  @Test
  @DisplayName("evaluates class qualified statics from a foreign frame")
  public void evaluatesClassQualifiedStaticsFromAForeignFrame() throws Exception {
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // top-level class statics read from another class's frame
    assertEquals("7", evaluated(frameId, "Config.version"), "Config.version");
    assertEquals("\"cfg\"", evaluated(frameId, "Config.title"), "Config.title");
    // a PACKAGED class: the dotted class prefix spans path segments
    assertEquals("99", evaluated(frameId, "pkg.Deep.marker"), "pkg.Deep.marker");
    // a function-typed static var (Null<Int->Void>) resolves by FQN — regression:
    // HFun statics were dropped as if they were methods, so this used to fail with
    // `"...Config" has no field "onBump"`
    assertTrue(evaluate(frameId, "Config.onBump").isSuccess(), "Config.onBump resolves");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("writes class qualified statics")
  public void writesClassQualifiedStatics() throws Exception {
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // writes resolve through the class prefix (restored right after: the
    // fixture's own output depends on Config.version)
    assertTrue(evaluate(frameId, "Config.version = 41").isSuccess(), "Config.version = 41");
    assertEquals("41", evaluated(frameId, "Config.version"), "written static reads back");
    assertTrue(evaluate(frameId, "Config.version = 7").isSuccess(), "Config.version restored");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("bare class name evaluates to its statics container")
  public void bareClassNameEvaluatesToItsStaticsContainer() throws Exception {
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    EvaluateResponse cls = evaluate(frameId, "Config");
    assertTrue(cls.getBody().getVariablesReference() > 0, "class itself is expandable");
    assertEquals("7", variablesByName(cls.getBody().getVariablesReference()).get("version"), "version listed under the class");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("unknown class root is rejected with the unresolved name code")
  public void unknownClassRootIsRejectedWithTheUnresolvedNameCode() throws Exception {
    runToBreakpoint(FIXTURE_MAIN, FIXTURE_INSPECT_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    Response unknown = evaluateRaw(frameId, "NoSuchClass.value");
    assertFalse(unknown.isSuccess(), "unknown class root rejected");
    assertTrue(unknown.getMessage().contains("NoSuchClass"), "message names the root");
    // machine-readable: the UnresolvedName code + the offending token, so a client
    // can resolve `NoSuchClass` against its own imports and re-issue qualified
    ErrorResponse err = (ErrorResponse)unknown;
    assertEquals(DebugErrorCode.UNRESOLVED_NAME.id(), err.getBody().getError().getId(), "UnresolvedName code");
    assertEquals("NoSuchClass", err.getBody().getError().getVariables().get("name"), "offending name in variables");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("evaluate rejects bad expressions with a clear message")
  public void evaluateRejectsBadExpressionsWithAClearMessage() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());
    Response unknown = evaluateRaw(frameId, "nosuch");
    assertFalse(unknown.isSuccess(), "unknown name must be rejected");
    assertTrue(unknown.getMessage().contains("nosuch"), "message names the variable");

    Response unknownInExpr = evaluateRaw(frameId, "nosuch + 1");
    assertFalse(unknownInExpr.isSuccess(), "unknown name inside an expression must be rejected");
    assertTrue(unknownInExpr.getMessage().contains("nosuch"), "message names the variable");

    Response malformed = evaluateRaw(frameId, "n +");
    assertFalse(malformed.isSuccess(), "malformed expression must be rejected");

    request(new DisconnectRequest());
  }

  // --- arbitrary expressions: operators folded adapter-side ---

  @Test
  @DisplayName("evaluates arithmetic and logic expressions")
  public void evaluatesArithmeticAndLogicExpressions() throws Exception {
    // Mutate.demo checkpoint: n=5, flag=false, obj=Point(1,2,"p"), arr=[5,10,15], idx=1
    runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // arithmetic on locals, precedence, parentheses
    assertEquals("6", evaluated(frameId, "n + 1"), "n + 1");
    assertEquals("11", evaluated(frameId, "n * 2 + 1"), "n * 2 + 1");
    assertEquals("12", evaluated(frameId, "(n + 1) * 2"), "(n + 1) * 2");
    assertEquals("2.5", evaluated(frameId, "n / 2"), "division is Float (Haxe)");
    assertEquals("-5", evaluated(frameId, "-n"), "unary minus");
    assertEquals("20", evaluated(frameId, "n << 2"), "shift");

    // fields, statics and class-qualified statics as operands
    assertEquals("3", evaluated(frameId, "obj.x + obj.y"), "obj.x + obj.y");
    // Config.bump() already ran by this point, so version is 8 (not the initial 7)
    assertEquals("13", evaluated(frameId, "Config.version + n"), "Config.version + n");

    // array elements, including a COMPUTED index
    assertEquals("11", evaluated(frameId, "arr[1] + 1"), "arr[1] + 1");
    assertEquals("10", evaluated(frameId, "arr[idx]"), "computed index arr[idx]");
    assertEquals("15", evaluated(frameId, "arr[idx + 1]"), "computed index arr[idx + 1]");

    // comparisons + logic (short-circuit)
    assertEquals("true", evaluated(frameId, "n > 4"), "n > 4");
    assertEquals("true", evaluated(frameId, "n == 5 && !flag"), "n == 5 && !flag");
    assertEquals("false", evaluated(frameId, "flag || n < 3"), "flag || n < 3");
    assertEquals("true", evaluated(frameId, "obj.label == \"p\""), "string content compare");

    // string concat
    assertEquals("\"n=5\"", evaluated(frameId, "\"n=\" + n"), "\"n=\" + n");
    assertEquals("\"p!\"", evaluated(frameId, "obj.label + \"!\""), "label concat");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("evaluates ternary and type checks")
  public void evaluatesTernaryAndTypeChecks() throws Exception {
    // Mutate.demo checkpoint: n=5, flag=false, obj=Point(1,2,"p"), arr=[5,10,15], idx=1
    runToBreakpoint(FIXTURE_MUTATE, FIXTURE_MUTATE_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // ternary — condition selects the branch, branches are full expressions
    assertEquals("\"pos\"", evaluated(frameId, "n > 0 ? \"pos\" : \"neg\""), "n > 0 ? ... : ...");
    assertEquals("2", evaluated(frameId, "flag ? 1 : 2"), "false condition takes else");
    assertEquals("10", evaluated(frameId, "n > 3 ? n * 2 : 0"), "branch is an expression");
    assertEquals("\"mid\"", evaluated(frameId, "n < 0 ? \"lo\" : n > 100 ? \"hi\" : \"mid\""), "ternary right-assoc chain");
    // only the taken branch is evaluated: the untaken branch names an unknown
    // variable, which would ERROR if it ran
    assertEquals("5", evaluated(frameId, "true ? n : nosuchvar"), "untaken branch is not evaluated");

    // `is` type checks
    assertEquals("true", evaluated(frameId, "obj is Point"), "object is its class");
    assertEquals("false", evaluated(frameId, "obj is String"), "object is not another class");
    assertEquals("true", evaluated(frameId, "n is Int"), "int is Int");
    assertEquals("true", evaluated(frameId, "n is Float"), "int is Float (Haxe)");
    assertEquals("false", evaluated(frameId, "n is Bool"), "int is not Bool");
    assertEquals("true", evaluated(frameId, "obj.label is String"), "field is String");

    // combined with logic
    assertEquals("true", evaluated(frameId, "obj is Point && n is Int"), "is in a boolean expression");

    // an unknown type name is a user error, not a silent false
    Response unknownType = evaluateRaw(frameId, "obj is Nonexistent");
    assertFalse(unknownType.isSuccess(), "unknown type rejected");
    assertTrue(unknownType.getMessage().contains("Nonexistent"), "names the unknown type");

    request(new DisconnectRequest());
  }

  // --- container-element writes + instance method calls ---

  @Test
  @DisplayName("writes array elements through evaluate")
  public void writesArrayElementsThroughEvaluate() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // arr[i] = x directly (element address is writable) — ints is [2,5,10]
    assertTrue(evaluate(frameId, "ints[0] = 99").isSuccess(), "ints[0] = 99");
    assertEquals("99", evaluated(frameId, "ints[0]"), "ints[0] now 99");
    // a path RHS into another element
    assertTrue(evaluate(frameId, "ints[2] = n").isSuccess(), "ints[2] = n");
    assertEquals("2", evaluated(frameId, "ints[2]"), "ints[2] now n (=2)");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("calls instance methods and mutates a map")
  public void callsInstanceMethodsAndMutatesAMap() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // read via a method call: stringMap has "a2"->2, "b"->6
    assertEquals("6", evaluated(frameId, "stringMap.get(\"b\")"), "stringMap.get(\"b\")");
    // intMap has 2->"v2"; String values are pointers (no boxing needed)
    assertEquals("\"v2\"", evaluated(frameId, "intMap.get(2)"), "intMap.get(2)");

    // MUTATE the map via its own method — the value-manipulation prize. A
    // String value is dynamic-compatible, so no boxing is required. The
    // insertion is proven by reading the new key back through get() (a missing
    // key returns null), the honest end-to-end signal.
    assertEquals("null", evaluated(frameId, "intMap.get(5)"), "absent key is null before insert");
    assertTrue(evaluate(frameId, "intMap.set(5, \"hi\")").isSuccess(), "intMap.set(5, \"hi\")");
    assertEquals("\"hi\"", evaluated(frameId, "intMap.get(5)"), "the inserted entry reads back");
    assertEquals("\"v2\"", evaluated(frameId, "intMap.get(2)"), "pre-existing entry intact");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("map bracket syntax sugars to get and set")
  public void mapBracketSyntaxSugarsToGetAndSet() throws Exception {
    // map[k] / map[k]=v are compile-time sugar for get/set; the evaluator
    // offers the same syntax by rewriting to the method calls.
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    // read: stringMap["b"]==6 (String key), intMap[2]=="v2" (Int key)
    assertEquals("6", evaluated(frameId, "stringMap[\"b\"]"), "stringMap[\"b\"]");
    assertEquals("\"v2\"", evaluated(frameId, "intMap[2]"), "intMap[2]");
    assertEquals("null", evaluated(frameId, "stringMap[\"zz\"]"), "absent key reads null");

    // write: string key with a boxed int value, and int key with a string value
    assertEquals("9", evaluated(frameId, "stringMap[\"c\"] = 9"), "stringMap[\"c\"] = 9 returns the value");
    assertEquals("9", evaluated(frameId, "stringMap[\"c\"]"), "stringMap[\"c\"] reads back");
    assertTrue(evaluate(frameId, "intMap[7] = \"seven\"").isSuccess(), "intMap[7] = \"seven\"");
    assertEquals("\"seven\"", evaluated(frameId, "intMap[7]"), "intMap[7] reads back");

    // arrays are NOT maps: arr[i] stays a real indexed slot, never rewritten
    // to get() (element writes are pinned by writesArrayElementsThroughEvaluate)
    assertEquals("5", evaluated(frameId, "ints[1]"), "ints[1] stays an index read");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("boxes primitives into dynamic arguments")
  public void boxesPrimitivesIntoDynamicArguments() throws Exception {
    // stringMap is Map<String,Int>: values are stored BOXED (Dynamic). Passing
    // the int literal 9 requires boxing it into a vdynamic.
    // Proven end to end: set then read the value back.
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());

    assertEquals("null", evaluated(frameId, "stringMap.get(\"c\")"), "absent before insert");
    assertTrue(evaluate(frameId, "stringMap.set(\"c\", 9)").isSuccess(), "stringMap.set(\"c\", 9) with boxing");
    assertEquals("9", evaluated(frameId, "stringMap.get(\"c\")"), "boxed int reads back");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("method call rejects unknown methods clearly")
  public void methodCallRejectsUnknownMethodsClearly() throws Exception {
    runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    int frameId = topFrameId(lastStoppedThreadId());
    Response noSuch = evaluateRaw(frameId, "stringMap.nope(1)");
    assertFalse(noSuch.isSuccess(), "unknown method rejected");
    Response wrongArity = evaluateRaw(frameId, "stringMap.set(\"c\")");
    assertFalse(wrongArity.isSuccess(), "wrong arity rejected");
    assertTrue(wrongArity.getMessage().contains("argument"), "arity message");

    request(new DisconnectRequest());
  }

  // --- instance methods ---

  @Test
  @DisplayName("statics scope appears in instance methods")
  public void staticsScopeAppearsInInstanceMethods() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);

    // stopped inside Point.move (an instance method): Point's statics must show
    int staticsRef = staticsScopeReference(topFrameId(stopped.getBody().getThreadId()));
    Map<String, String> statics = variablesByName(staticsRef);
    assertEquals("2", statics.get("axes"), "Point.axes");
    // compiler bookkeeping like __name__ must be hidden
    for (String name : statics.keySet()) {
      assertFalse(name.startsWith("__") && name.endsWith("__"), "compiler field leaked into Statics: " + name);
    }

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("shows this in instance method")
  public void showsThisInInstanceMethod() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_POINT, FIXTURE_POINT_METHOD_LINE);

    // stopped inside Point.move: `this` must be listed and expand to the Point
    Variable self = findVariable(topFrameVariables(stopped.getBody().getThreadId()), "this");
    assertNotNull(self, "`this` present in an instance-method frame");
    assertTrue(self.getVariablesReference() > 0, "`this` is expandable");
    Map<String, String> fields = variablesByName(self.getVariablesReference());
    assertEquals("10", fields.get("x"), "this.x (move not applied yet)");
    assertEquals("20", fields.get("y"), "this.y");

    request(new DisconnectRequest());
  }

  private static int countByName(List<Variable> variables, String name) {
    int count = 0;
    for (Variable variable : variables) {
      if (name.equals(variable.getName())) count++;
    }
    return count;
  }

  private static Variable findByNameSuffix(List<Variable> variables, String suffix) {
    for (Variable variable : variables) {
      if (variable.getName() != null && variable.getName().endsWith(suffix)) return variable;
    }
    return null;
  }

  /** Stops at FIXTURE_RICH_LINE and returns Rich.demo's locals. */
  private List<Variable> richLocals() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_RICH, FIXTURE_RICH_LINE);
    return topFrameVariables(stopped.getBody().getThreadId());
  }
}
