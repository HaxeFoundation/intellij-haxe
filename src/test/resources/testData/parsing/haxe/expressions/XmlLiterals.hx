class XmlLiterals {
    public function new() {
        var basic = <xml></xml>;
        var basicSpace = <xml ></xml>;
        var basicSpaceContent = <xml > </xml>;
        var nestedSameName = <xml><xml></xml></xml>;
        var nestedDifferentName = <xml><yml></xml>;
        var selfClose = <xml/>;
        var selfCloseWithAttr = <xml abc />;
        var nestedSelfClose = <xml><xml /></xml>;
        var hyphenName = <xml-xml></xml-xml>;
        var colonPrefixName = <:xml></:xml>;
        var colonInName = <xml:xml></xml:xml>;
        var complexName = <foo.Bar_barf3-gnieh:blargh></foo.Bar_barf3-gnieh:blargh>;
        var fragment = <></>;
        var fragmentWithContent = <>abc</>;
        var interpolated = <xml>$count + $count = ${count*2}</xml>;
        var nestedInInterpolation = <xml>${<inner></inner>}</xml>;
        var dollarName = <$xml></$xml>;
        var uppercaseName = <Xml></Xml>;

        // regression guards: these must keep parsing exactly as before (generics/comparisons).
        var generic:Array<Int> = [];
        var comparison = a < b;
        var regexCompare = ~/x/ < 1;
    }

    public function generic<T>(value:T):T {
        return value;
    }
}
