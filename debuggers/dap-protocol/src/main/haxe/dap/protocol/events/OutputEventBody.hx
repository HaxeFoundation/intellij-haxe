package dap.protocol.events;

/**
	Body of the "output" event.
	`category` is "stdout" or "stderr" for forwarded debuggee output.
**/
typedef OutputEventBody = {
	var output:String;
	var ?category:String;
}
