<mongoConnection> << EOF <grepFunction> > <fileResultName>


    /**
     * Convert JSON object to String.
     */
    function toJsonObj(x, indent, nolint){
    var lineEnding = nolint ? " " : "\n";
    var tabSpace = nolint ? "" : "\t";
    assert.eq((typeof x), "object", "tojsonObject needs object, not [" + (typeof x) + "]");

    if (!indent)
    indent = "";

    if (typeof(x.tojson) == "function" && x.tojson != tojson) {
    return x.tojson(indent,nolint);
}

    if (x.constructor && typeof(x.constructor.tojson) == "function" && x.constructor.tojson != tojson) {
    return x.constructor.tojson(x, indent, nolint);
}

    if (x instanceof Error) {
    return x.toString();
}

    try {
    x.toString();
}
    catch(e) {
    // toString not callable
    return "[object]";
}

    var s = "{" + lineEnding;

    // push one level of indent
    indent += tabSpace;

    var keys = x;
    if (typeof(x._simpleKeys) == "function")
    keys = x._simpleKeys();
    var fieldStrings = [];
    for (var k in keys){
    var val = x[k];

    // skip internal DB types to avoid issues with interceptors
    if (typeof DB != 'undefined' && val == DB.prototype)
    continue;
    if (typeof DBCollection != 'undefined' && val == DBCollection.prototype)
    continue;

    fieldStrings.push(indent + "\"" + k + "\" : " + toJsonLine(val, indent, nolint));
}

    if (fieldStrings.length > 0) {
    s += fieldStrings.join("," + lineEnding);
}
    else {
    s += indent;
}
    s += lineEnding;

    // pop one level of indent
    indent = indent.substring(1);
    return s + indent + "}";
}

    /**
     * Print JSON in one line.
     */
    function toJsonLine(x, indent, nolint){
    if (x === null)
    return "null";

    if (x === undefined)
    return "undefined";

    if (!indent)
    indent = "";

    switch (typeof x) {
    case "string":
    return tojson(x);
    case "number":
    if(x % 1 != 0){
        return "" + x;
    }else{
        return "" + x + ".0";
    }
    case "boolean":
    return "" + x;
    case "object":{
    var s = toJsonObj(x, indent, nolint);
    if ((nolint == null || nolint == true) &&
    s.length < 80 &&
    (indent == null || indent.length == 0)){
    s = s.replace(/[\t\r\n]+/gm, " ");
}
    return s;
}
    case "function":
    if (x === MinKey || x === MaxKey)
    return x.tojson();
    return x.toString();
    default:
    throw Error( "tojson can't handle type " + (typeof x) );
}
}

    function printJsonLine(x){
        print(toJsonLine(x,"",true));
    }

try{
    db.getMongo().setReadPref("secondary");
    var cursor = db.<collectionName>.<operationDefinition>;
    if(<addCount>){
        var total = <totalOperation>;
        if(typeof total == 'object'){
            var total = (total.hasNext())? total.next().count : 0;
        }
        DBQuery.shellBatchSize = total;
        print(total);
    } else {
        DBQuery.shellBatchSize = 0;
    }

    if(<enableExplain>){
        printJsonLine(<explainDefinition>);
    }
    if(<addQuery>){
        var cursorFind = cursor<queryOptions><cursorComment>;
        cursorFind.forEach(function(row){ printJsonLine(row); });
    }
}catch(e){
    if(e.message.contains("error:")){
        printjsononeline( JSON.parse("{" + e.message.replace(/NumberLong\(\d+\)/, 0).replace("error:", "\"error\":") + "}"));
    }else{
        printjsononeline({ "error": { "ok": 0, "errmsg": e.message}});
    }
}
EOF