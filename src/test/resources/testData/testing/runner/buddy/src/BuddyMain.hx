import buddy.*;
using buddy.Should;

class BuddyMain extends SingleSuite {
  public function new() {
    describe("A calculator", {
      it("adds numbers", {
        (1 + 1).should.be(2);
      });
      it("traces while working", {
        trace("a trace from buddy");
        (2 * 2).should.be(4);
      });
      it("fails sometimes", {
        (1 + 1).should.be(3);
      });
      it("is pending later");
      describe("nested memory bank", {
        it("stores values", {
          "x".should.be("x");
        });
      });
    });
  }
}
