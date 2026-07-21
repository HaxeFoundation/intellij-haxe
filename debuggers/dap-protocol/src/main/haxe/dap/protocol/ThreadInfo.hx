package dap.protocol;

/**
	A thread descriptor, returned in the "threads" response.
	(Named ThreadInfo because the DAP type name "Thread" collides with sys.thread.Thread.)
**/
typedef ThreadInfo = {
	var id:Int;
	var name:String;
}
