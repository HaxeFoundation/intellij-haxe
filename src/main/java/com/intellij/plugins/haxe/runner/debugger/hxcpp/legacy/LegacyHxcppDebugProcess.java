package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils;
import com.intellij.plugins.haxe.runner.debugger.HaxeBreakpointType;
import com.intellij.plugins.haxe.runner.debugger.HaxeDebuggerEditorsProvider;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.concurrency.QueueProcessor;
import com.intellij.util.io.URLUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.*;
import debugger.Command;
import debugger.FrameList;
import debugger.Message;
import debugger.StringList;
import debugger.StructuredValue;
import debugger.StructuredValueList;
import debugger.StructuredValueListType;
import debugger.StructuredValueType;
import debugger.ThreadWhereList;
import haxe.root.JavaProtocol;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The legacy HXCPP debug process: listens on a server socket for the debuggee
 * (a hxcpp program built with the old {@code debugger} haxelib) to connect,
 * then drives the session by message passing over the {@code hxcpp.DebugSocket}
 * protocol ({@code debugger.Command}/{@code debugger.Message} from the
 * {@code :debuggers:hxcpp-debugger-protocol-legacy} module).
 *
 * The very first ThreadStopped after connect is the debuggee waiting in
 * {@code -start_debugger} startup: the deferred command queue (commands issued
 * before the socket existed, breakpoints above all) is flushed there and the
 * program resumed, so it never surfaces as a user-visible stop.
 */
public class LegacyHxcppDebugProcess extends XDebugProcess {

  /** Handles the response message to one enqueued command. */
  @FunctionalInterface
  private interface MessageListener {
    void handleMessage(int messageId, Message message);
  }

  private final Project project;
  private final Module module;
  private final LinkedList<Pair<Command, MessageListener>> deferredQueue = new LinkedList<>();
  private final LinkedList<MessageListener> listenerQueue = new LinkedList<>();
  private final QueueProcessor<Runnable> writeQueue =
    QueueProcessor.createRunnableQueueProcessor(QueueProcessor.ThreadToUse.POOLED);
  private final XBreakpointHandler<?>[] breakpointHandlers;
  private final Map<XLineBreakpoint<XBreakpointProperties>, Integer> breakpointIds = new HashMap<>();
  private ServerSocket serverSocket;
  private Socket debugSocket;
  // stop() closes the sockets, which makes the blocking accept()/readMessage in
  // readLoop() throw; the flag lets start()'s catch tell that apart from a failure.
  private volatile boolean stopped;
  private boolean stoppedOnce;
  private ExecutionResult executionResult;

  public LegacyHxcppDebugProcess(@NotNull XDebugSession session, Module module, int port) throws IOException {
    super(session);
    this.project = session.getProject();
    this.module = module;
    serverSocket = new ServerSocket(port);
    breakpointHandlers = createBreakpointHandlers();
  }

  public void setExecutionResult(ExecutionResult executionResult) {
    this.executionResult = executionResult;
  }

  public void start() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        readLoop();
      }
      catch (Throwable t) {
        if (stopped) {
          return;
        }
        SwingUtilities.invokeLater(() -> error(HaxeDebuggerBundle.message("legacy.hxcpp.error.loop.failed", t)));
      }
    });
  }

  @Override
  protected ProcessHandler doGetProcessHandler() {
    return executionResult == null ? null : executionResult.getProcessHandler();
  }

  @Override
  @NotNull
  public ExecutionConsole createConsole() {
    return executionResult == null ? super.createConsole() : executionResult.getExecutionConsole();
  }

  @Override
  @NotNull
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    return breakpointHandlers;
  }

  @Override
  @NotNull
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new HaxeDebuggerEditorsProvider();
  }

  @Override
  public void startPausing() {
    expectOK(Command.BreakNow);
  }

  @Override
  public void resume(@Nullable XSuspendContext context) {
    expectOK(Command.Continue(1));
  }

  @Override
  public void startStepOver(@Nullable XSuspendContext context) {
    expectOK(Command.Next(1));
  }

  @Override
  public void startStepInto(@Nullable XSuspendContext context) {
    expectOK(Command.Step(1));
  }

  @Override
  public void startStepOut(@Nullable XSuspendContext context) {
    expectOK(Command.Finish(1));
  }

  @Override
  public void stop() {
    stopped = true;
    synchronized (this) {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        }
        catch (IOException ignored) {
        }
        serverSocket = null;
      }
      if (debugSocket != null) {
        try {
          debugSocket.close();
        }
        catch (IOException ignored) {
        }
        debugSocket = null;
      }
      // Stop the write queue. Otherwise we get a bunch of pointless dialogs.
      writeQueue.dismissLastTasks(0);
    }
  }

  @Override
  public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
    // TODO: not supported by this protocol without breakpoint juggling
  }

  // --- user-facing messages ---

  void info(String message) {
    notify(message, NotificationType.INFORMATION);
  }

  private void warn(String message) {
    notify(message, NotificationType.WARNING);
  }

  private void error(String message) {
    notify(message, NotificationType.ERROR);
    stop();
  }

  private void notify(String message, NotificationType type) {
    NotificationGroupManager.getInstance()
      .getNotificationGroup("haxe.debugger.legacy")
      .createNotification(HaxeDebuggerBundle.message("legacy.hxcpp.notification.title"), message, type)
      .notify(project);
  }

  // --- protocol plumbing ---

  private void expectOK(Command command) {
    enqueueCommand(command, (messageId, message) -> {
      if (messageId != JavaProtocol.IdOK) {
        error(HaxeDebuggerBundle.message("legacy.hxcpp.error.unexpected.message", "OK", JavaProtocol.messageToString(message)));
      }
    });
  }

  private void where() {
    enqueueCommand(Command.WhereCurrentThread(false), (messageId, message) -> {
      if (messageId != JavaProtocol.IdThreadsWhere) {
        error(HaxeDebuggerBundle.message("legacy.hxcpp.error.unexpected.message", "IdThreadsWhere", JavaProtocol.messageToString(message)));
        return;
      }
      getSession().positionReached(new SuspendContext(message));
    });
  }

  private void enqueueCommand(Command command, MessageListener listener) {
    try {
      synchronized (this) {
        if (debugSocket == null) {
          deferredQueue.add(Pair.create(command, listener));
          return;
        }
        listenerQueue.add(listener);
        OutputStream os = debugSocket.getOutputStream();
        writeQueue.add(() -> {
          try {
            JavaProtocol.writeCommand(os, command);
          }
          catch (RuntimeException e) {
            error(HaxeDebuggerBundle.message("legacy.hxcpp.error.write.failed", JavaProtocol.commandToString(command), e));
          }
        });
      }
    }
    catch (IOException e) {
      error(HaxeDebuggerBundle.message("legacy.hxcpp.error.queue.failed", JavaProtocol.commandToString(command), e));
    }
  }

  private void readLoop() throws IOException {
    ServerSocket listening;
    synchronized (this) {
      listening = serverSocket;
    }
    // Don't synchronize around the accept. It locks up the rest of the debugger
    // still running on the AWT thread if the application isn't starting correctly.
    Socket accepted = listening.accept();
    synchronized (this) {
      debugSocket = accepted;
      serverSocket.close();
      serverSocket = null;
      JavaProtocol.readClientIdentification(debugSocket.getInputStream());
      JavaProtocol.writeServerIdentification(debugSocket.getOutputStream());
    }
    while (true) {
      Socket socket;
      synchronized (this) {
        socket = debugSocket;
      }
      if (socket == null) {
        break;
      }
      Message message = JavaProtocol.readMessage(socket.getInputStream());
      int messageId = JavaProtocol.getMessageId(message);
      if (messageId == JavaProtocol.IdThreadCreated ||
          messageId == JavaProtocol.IdThreadTerminated ||
          messageId == JavaProtocol.IdThreadStarted) {
        continue;
      }
      if (messageId == JavaProtocol.IdThreadStopped) {
        if (stoppedOnce) {
          // Solicit the current thread's stack frames
          where();
        }
        else {
          stoppedOnce = true;
          while (!deferredQueue.isEmpty()) {
            Pair<Command, MessageListener> deferred = deferredQueue.removeFirst();
            enqueueCommand(deferred.getFirst(), deferred.getSecond());
          }
          resume(null);
        }
        continue;
      }
      MessageListener listener;
      synchronized (this) {
        listener = listenerQueue.isEmpty() ? null : listenerQueue.removeFirst();
      }
      if (listener == null) {
        error(HaxeDebuggerBundle.message("legacy.hxcpp.error.unsolicited", JavaProtocol.messageToString(message)));
        break;
      }
      listener.handleMessage(messageId, message);
    }
  }

  // --- breakpoints ---

  private void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
    XSourcePosition position = breakpoint.getSourcePosition();
    if (position == null) {
      return;
    }

    String path = ReadAction.computeBlocking(() -> getRelativePath(project, position.getFile()));

    enqueueCommand(Command.AddFileLineBreakpoint(path, position.getLine() + 1), (messageId, message) -> {
      if (messageId == JavaProtocol.IdFileLineBreakpointNumber) {
        breakpointIds.put(breakpoint, (Integer)message.params[0]);
      }
      else {
        getSession().updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_invalid_breakpoint, null);
        warn(HaxeDebuggerBundle.message("legacy.hxcpp.warn.breakpoint.not.set"));
      }
    });
  }

  private void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
    Integer id = breakpointIds.remove(breakpoint);
    if (id == null) {
      return;
    }
    enqueueCommand(Command.DeleteBreakpointRange(id, id), (messageId, message) -> {
      // Could verify that the response was Deleted ...
    });
  }

  private XBreakpointHandler<?>[] createBreakpointHandlers() {
    return new XBreakpointHandler<?>[]{
      new XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>>(HaxeBreakpointType.class) {
        @Override
        public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
          LegacyHxcppDebugProcess.this.registerBreakpoint(breakpoint);
        }

        @Override
        public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
          LegacyHxcppDebugProcess.this.unregisterBreakpoint(breakpoint);
        }
      }
    };
  }

  /**
   * The breakpoint path the debuggee's runtime knows: package path + file name
   * (the protocol matches on source-relative paths, not absolute ones).
   */
  private static String getRelativePath(Project project, VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    String packageName = HaxeResolveUtil.getPackageName(psiFile);
    String fileName = VfsUtil.extractFileName(file.getPath());
    if (StringUtil.isEmpty(packageName)) {
      return fileName;
    }
    // every literal dot in the package name becomes a path separator
    return packageName.replaceAll("\\.", "/") + "/" + fileName;
  }

  // --- the suspended-state model ---

  private class SuspendContext extends XSuspendContext {
    private final XExecutionStack[] executionStacks;

    SuspendContext(Message threadsWhereMessage) {
      List<XExecutionStack> stacks = new LinkedList<>();
      ThreadWhereList whereList = (ThreadWhereList)threadsWhereMessage.params[0];
      while (whereList != ThreadWhereList.Terminator) {
        int number = (Integer)whereList.params[0];
        FrameList frameList = (FrameList)whereList.params[2];
        stacks.add(new ExecutionStack(number, frameList));
        whereList = (ThreadWhereList)whereList.params[3];
      }
      executionStacks = stacks.toArray(new XExecutionStack[0]);
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
      return executionStacks.length > 0 ? executionStacks[0] : null;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
      return executionStacks;
    }
  }

  private class ExecutionStack extends XExecutionStack {
    private final List<XStackFrame> stackFrames = new LinkedList<>();

    ExecutionStack(int number, FrameList frameList) {
      super("Thread " + number);
      while (frameList != FrameList.Terminator) {
        stackFrames.add(new StackFrame(frameList));
        frameList = (FrameList)frameList.params[6];
      }
    }

    @Override
    public XStackFrame getTopFrame() {
      return stackFrames.isEmpty() ? null : stackFrames.get(0);
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
      if (firstFrameIndex < stackFrames.size()) {
        container.addStackFrames(stackFrames.subList(firstFrameIndex, stackFrames.size()), true);
      }
    }
  }

  private class StackFrame extends XStackFrame {
    private final int frameNumber;
    private final String fileName;
    private final int lineNumber;
    private final String classAndFunctionName;
    private final XSourcePosition sourcePosition;

    StackFrame(FrameList frameList) {
      frameNumber = (Integer)frameList.params[1];
      fileName = (String)frameList.params[4];
      lineNumber = (Integer)frameList.params[5];
      classAndFunctionName = frameList.params[2] + "." + frameList.params[3];

      VirtualFile file = findSourceFile();
      // IDEA treats symlinks as separate files; collapse to the canonical one.
      if (file != null) {
        file = HaxeFileUtil.getCanonicalFile(file);
      }
      sourcePosition = file == null ? null : XDebuggerUtil.getInstance().createPosition(file, Math.max(0, lineNumber - 1));
    }

    @Nullable
    private VirtualFile findSourceFile() {
      VirtualFileManager vfm = VirtualFileManager.getInstance();
      VirtualFile file = vfm.findFileByUrl(vfm.constructUrl(URLUtil.FILE_PROTOCOL, fileName));
      if (file != null && file.exists()) {
        return file;
      }

      // Filename index can only deal with the name, not any paths.
      String name = VfsUtil.extractFileName(fileName);
      String fileNameToLookFor = name == null ? fileName : name;
      Collection<VirtualFile> files = ReadAction.computeBlocking(() -> findByName(fileNameToLookFor));

      Collection<VirtualFile> matches = new HashSet<>();
      for (VirtualFile candidate : files) {
        if (candidate.getPath().endsWith(fileName)) {
          matches.add(candidate);
        }
      }
      if (matches.isEmpty()) {
        // No match yet: walk the classpath looking for an appropriate file.
        return HaxelibClasspathUtils.findFileOnClasspath(module, fileName);
      }
      if (matches.size() == 1) {
        // Got one. If it's a good file, keep it; otherwise try the classpath.
        VirtualFile possible = matches.iterator().next();
        return possible.isValid() ? possible : HaxelibClasspathUtils.findFileOnClasspath(module, possible.toString());
      }
      // Too many matches. Get the first that occurs on the classpath.
      return HaxelibClasspathUtils.findFirstFileOnClasspath(module, matches);
    }

    private Collection<VirtualFile> findByName(String name) {
      Collection<VirtualFile> files =
        FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.moduleScope(module));
      if (files.isEmpty()) {
        files = FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.moduleWithDependenciesScope(module));
      }
      if (files.isEmpty()) {
        files = FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.moduleWithLibrariesScope(module));
      }
      if (files.isEmpty()) {
        files = FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.allScope(project));
      }
      return files;
    }

    @Override
    public Object getEqualityObject() {
      return (fileName + classAndFunctionName).intern();
    }

    @Override
    public XDebuggerEvaluator getEvaluator() {
      return new XDebuggerEvaluator() {
        @Override
        public void evaluate(@NotNull String expression,
                             @NotNull XEvaluationCallback callback,
                             XSourcePosition expressionPosition) {
          callback.evaluated(new Value(expression));
        }
      };
    }

    @Override
    public XSourcePosition getSourcePosition() {
      return sourcePosition;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
      // Move to the stack frame first - Variables reads the current frame
      enqueueCommand(Command.SetFrame(frameNumber), (messageId, message) -> {
        if (messageId == JavaProtocol.IdThreadLocation) {
          computeChildrenCurrentFrame(node);
        }
        else {
          warn(HaxeDebuggerBundle.message("legacy.hxcpp.warn.frame.not.set", frameNumber, JavaProtocol.messageToString(message)));
        }
      });
    }

    @Override
    public void customizePresentation(@NotNull ColoredTextContainer component) {
      SimpleTextAttributes attributes = sourcePosition == null ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                                                               : SimpleTextAttributes.REGULAR_ATTRIBUTES;
      component.append(classAndFunctionName + "  [" + fileName + ":" + lineNumber + "]", attributes);
      component.setIcon(AllIcons.Debugger.Frame);
    }

    private void computeChildrenCurrentFrame(@NotNull XCompositeNode node) {
      enqueueCommand(Command.Variables(false), (messageId, message) -> {
        if (messageId == JavaProtocol.IdVariables) {
          XValueChildrenList childrenList = new XValueChildrenList();
          StringList stringList = (StringList)message.params[0];
          while (stringList != StringList.Terminator) {
            String name = (String)stringList.params[0];
            if (!isIntermediateVariableName(name)) {
              childrenList.add(name, new Value(name));
            }
            stringList = (StringList)stringList.params[1];
          }
          node.addChildren(childrenList, true);
        }
        else {
          warn(HaxeDebuggerBundle.message("legacy.hxcpp.warn.variables.failed", JavaProtocol.messageToString(message)));
        }
      });
    }

    /** Whether a variable name has been introduced by the compiler's target backend (e.g. hxcpp). */
    private boolean isIntermediateVariableName(String s) {
      return s.startsWith("_g");
    }

    private class Value extends XValue {
      private final String name;
      private String expression;
      private Icon icon;
      private String type;
      private String value;
      private List<Value> children;

      // The children may be requested by the UI before the structured value
      // has arrived; the pending node is remembered and filled on arrival.
      private boolean waitingForChildrenResults;
      private XCompositeNode pendingChildrenNode;

      Value(String name) {
        this.name = name;
        this.expression = name;
      }

      @Override
      public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        // No icon yet means the value has not been fetched
        if (icon == null) {
          fetchValue(node);
          return;
        }
        setPresentation(node);
      }

      private void setPresentation(@NotNull XValueNode node) {
        node.setPresentation(icon, type, value, children != null);
      }

      @Override
      public String getEvaluationExpression() {
        return expression;
      }

      @Override
      public boolean canNavigateToSource() {
        return false;
      }

      @Override
      public boolean canNavigateToTypeSource() {
        return false;
      }

      @Override
      public void computeChildren(@NotNull XCompositeNode node) {
        if (children == null || waitingForChildrenResults) {
          pendingChildrenNode = node;
          return;
        }
        addChildrenTo(node);
      }

      private void addChildrenTo(@NotNull XCompositeNode node) {
        XValueChildrenList childrenList = new XValueChildrenList(children.size());
        for (Value child : children) {
          childrenList.add(child.name, child);
        }
        node.addChildren(childrenList, true);
      }

      private void fetchValue(@NotNull XValueNode node) {
        waitingForChildrenResults = true;
        enqueueCommand(Command.GetStructured(false, expression), (messageId, message) -> {
          if (messageId == JavaProtocol.IdStructured) {
            fromStructuredValue((StructuredValue)message.params[0]);
          }
          else {
            icon = AllIcons.General.Error;
            value = type = "<Unavailable - " + stripErrorAdornments(message.toString()) + ">";
          }

          waitingForChildrenResults = false;
          XCompositeNode pending = pendingChildrenNode;
          pendingChildrenNode = null;
          if (pending != null && children != null) {
            addChildrenTo(pending);
          }

          setPresentation(node);
        });
      }

      private void fromStructuredValue(StructuredValue structuredValue) {
        if (structuredValue.index == 0) {  // Elided
          expression = (String)structuredValue.params[1];
        }
        else if (structuredValue.index == 1) {  // Single
          icon = AllIcons.Debugger.Value;
          type = getTypeString((StructuredValueType)structuredValue.params[0]);
          value = stripErrorAdornments((String)structuredValue.params[1]);
        }
        else if (structuredValue.index == 2) {  // List
          icon = AllIcons.Debugger.Value;
          type = getTypeString((StructuredValueListType)structuredValue.params[0]);
          value = "";
          children = new LinkedList<>();
          StructuredValueList list = (StructuredValueList)structuredValue.params[1];
          while (list != StructuredValueList.Terminator) {
            Value child = new Value((String)list.params[0]);
            child.fromStructuredValue((StructuredValue)list.params[1]);
            children.add(child);
            list = (StructuredValueList)list.params[2];
          }
        }
        else {
          icon = AllIcons.General.Error;
          value = type = "<Unavailable>";
        }
      }

      private String getTypeString(StructuredValueType type) {
        return switch (type.index) {
          case 0 -> "Null";
          case 1 -> "Bool";
          case 2 -> "Int";
          case 3 -> "Float";
          case 4 -> "String";
          case 5, 6, 8 -> (String)type.params[0];
          case 7 -> "{ ... }";
          case 9 -> "Function";
          default -> "<Unavailable>";
        };
      }

      private String getTypeString(StructuredValueListType type) {
        return switch (type.index) {
          case 0 -> "{ ... }";
          case 1 -> (String)type.params[0];
          case 2 -> "Array";
          default -> "<Unavailable>";
        };
      }

      private String stripErrorAdornments(String message) {
        // the debuggee wraps evaluation failures as
        // ErrorEvaluatingExpression(<description>\nCalled from <stack>...) -
        // unwrap the description and drop the call stack
        Pattern wrapperPattern = Pattern.compile("ErrorEvaluatingExpression\\((.*)\\)", Pattern.DOTALL);
        Matcher m = wrapperPattern.matcher(message);
        String description = m.matches() ? m.group(1) : message;

        final String callStackMarker = "\nCalled from ";
        if (description.contains(callStackMarker)) {
          description = description.substring(0, description.indexOf(callStackMarker));
        }
        return description;
      }
    }
  }
}
