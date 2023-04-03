class InternalAnonymousLocalFunctionDeclaration {
    function reduced () {
        var a = if (true) { function() return false; }  // <<--- Error was at open parens
    }
}