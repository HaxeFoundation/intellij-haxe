package ijhaxe.adapter;

import ijhaxe.debug.session.DebugEvent;


/**
	A message for the worker thread, which runs the dispatcher. Client frames
	and session events share one queue, so the dispatcher stays single-threaded
	and sees all messages in one order.
**/
enum WorkerMessage {
	ClientPayload(payload:String);
	FromSession(event:DebugEvent);
	ClientEof;
}
