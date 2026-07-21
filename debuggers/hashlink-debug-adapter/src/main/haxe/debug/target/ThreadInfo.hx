package debug.target;

/**
	A live debuggee thread: its OS id (as used by wait/read_register) and a display name.
**/
typedef ThreadInfo = {
	var id:Int;
	var name:String;
}
