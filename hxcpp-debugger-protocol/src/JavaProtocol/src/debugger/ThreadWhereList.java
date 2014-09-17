package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class ThreadWhereList extends haxe.lang.Enum
{
	static 
	{
		debugger.ThreadWhereList.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Terminator", "Where"});
		debugger.ThreadWhereList.Terminator = new debugger.ThreadWhereList(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    ThreadWhereList(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.ThreadWhereList Terminator;
	
	public static   debugger.ThreadWhereList Where(int number, debugger.ThreadStatus status, debugger.FrameList frameList, debugger.ThreadWhereList next)
	{
		return new debugger.ThreadWhereList(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, status, frameList, next})) ));
	}
	
	
}


