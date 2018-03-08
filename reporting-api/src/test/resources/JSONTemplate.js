<mongoConnection> << EOF <grepFunction> > <fileResultName>

    function getValueByPath(obj, path) {
    var a = path.split('.');
    for (var i = 0, n = a.length; i < n; ++i) {
    var k = a[i];
    if (k in obj) {
    obj = obj[k];
} else {
    return;
}
}
    return obj;
}

    function formatValue(key, value){
    if(value ==='')
    return value;
    if(!value)
    return null;
    switch (key) {
    case "formatDate":
    return formatDate(value); break;
    case "formatDwellTime":
    return formatDwellTime(value); break;
    case "none": {
    if (typeof(value) == "number" && value % 1 != 0)
    return value.valueOf() + ".0";
    return value.valueOf();
    break;
}
}
}

    function formatDwellTime(value) {
    var sign = (value < 0) ? "-" : "";
    value = (value < 0) ? value * -1 : value;
    //print("value = " + value);
    var x = Math.floor(value / 1000);
    //print("x =" + x);
    var seconds = Math.floor(x % 60);
    //print("seconds = " + seconds);

    x = (x / 60);
    var minutes = Math.floor(x % 60);
    //print("minutes = " + minutes);

    x = (x / 60);
    var hours = Math.floor(x % 24);
    x = (x / 24);
    var days = Math.floor(x);
    var hoursS = (hours < 10 ? "0" + hours + ":" : hours + ":");
    var minutesS = (minutes < 10 ? "0" + minutes + ":" : minutes + ":");
    var secondsS = seconds < 10 ? "0" + seconds : seconds + "";
    return sign + days + " Days " + hoursS + minutesS + secondsS;
}

    function formatDate(value) {
    return value.toLocaleDateString("en-US") + " " + value.toLocaleTimeString("en-US");
}

    db.getMongo().setReadPref("secondary");
    var cursor = db.<collectionName>.<operationDefinition>;
    var total = <totalOperation>;
    if(typeof total == 'object'){
        var total = (total.hasNext())? total.next().count : 0;
    }
    print("{ totalRows: ");
        DBQuery.shellBatchSize = total;
        print(", data: [ ");
        <alias>
        var cursorFind = cursor<queryOptions><cursorComment>;
        var count = 1;
        cursor.forEach(
        function(row){
            var formatRow = {};
            for(var index in alias){
            var value = getValueByPath(row,index);
            if(value !== undefined){
            formatRow[alias[index]["alias"]] = formatValue(alias[index]["function"],value);
        }
        }
            if(count < total) {
            printjsononeline(formatRow);
            print(",");
        } else {
            printjsononeline(formatRow);
        }
            count++;
        }
        );
        print(" ]");
        print("}");
EOF