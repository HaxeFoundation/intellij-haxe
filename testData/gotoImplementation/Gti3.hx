interface Test<caret> {}

class A implements Test {}
class B implements Test {}

// assumption: because of list will update by backround task,
// first list of targets will be not completed
//class C implements Test {}
//class D implements Test {}