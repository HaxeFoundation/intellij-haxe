package;

import haxe.ds.StringMap;

class MapInitializerParsingIssue {
    private static var VAR : StringMap<String> = [
        "Something" => "Nothing",
        "Other"     => "Else"
    ];

    private static var MAP: Map<String, String> = [
        "This" => "That",
        "What" => "Where"
    ];

    public function new() {
      VAR = MAP;
    }
}
