package debug.session;

/**
 * One stack frame resolved to source coordinates for a "stackTrace" response.
 */
typedef FrameInfo = {
	var id:Int;
	var name:String;
	var file:Null<String>;
	var line:Int;
}
