import com.util.ClassFactory;
class PrivateMethod {
  private function print(factory:ClassFactory){
    factory.get<caret>;
  }
}
