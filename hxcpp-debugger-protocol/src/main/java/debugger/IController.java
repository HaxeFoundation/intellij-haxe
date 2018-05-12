package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  interface IController extends haxe.lang.IHxObject
{
	   debugger.Command getNextCommand();
	
	   void acceptMessage(debugger.Message message);
	
}


