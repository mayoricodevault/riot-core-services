/**
 * Created by dbascope on 9/30/16.
 *
 */
db.system.js.save(
    {
        _id: "JSONFormatter",
        value : function (inputJSON) {
            function cleanField(field){
                if(!field && field !=0 && field != true){
                    return "";
                }
                var out = field.toString();
                if (out.indexOf("NumberLong") !== -1 || out.indexOf("NumberInt") !== -1 || out.indexOf("Timestamp") !== -1){
                    out = out.replace(/NumberLong\((.*?)\)/g, "$1");
                    out = out.replace(/NumberInt\((.*?)\)/g, "$1");
                    out = out.replace(/Timestamp\((.*?)\)/g, "$1");
                    return parseInt(out.replace(/"/g,""));
                } else {
                    out = out.replace(/ISODate\((.*?)\)/g, "$1");
                    out = out.replace(/ObjectId\((.*?)\)/g, "$1");
                    out = out.replace(/BinData\((.*?)\)/g, "$1");
                    out = out.replace(undefined, "$1");
                    return out;
                }
            }

            var isExport = options.export.toString().toLowerCase() == "true";

            for (var key in inputJSON.options){
                if (inputJSON.options.hasOwnProperty(key)){
                    inputJSON.options[key] = cleanField(inputJSON.options[key]);
                }
            }
            if (inputJSON.hasOwnProperty("columnNames") && isExport){
                var header = inputJSON.hasOwnProperty("labelY") ? inputJSON.labelY + "," : ",";
                print ((inputJSON.hasOwnProperty("rowNames")?  header:"")+inputJSON.columnNames);
            }
            var rowIdx = 0;
            for (var row=0;row< inputJSON.data.length;row++){
                for (var key in inputJSON.data[row]){
                    if (inputJSON.data[row].hasOwnProperty(key)){
                        inputJSON.data[row][key] = cleanField(inputJSON.data[row][key]);
                    }
                }
                if (isExport){
                    print ((inputJSON.hasOwnProperty("rowNames")?inputJSON.rowNames[rowIdx]+",":"")+inputJSON.data[row]);
                    rowIdx++;
                }
            }
            if (!isExport){
                return inputJSON;
            }
        }
    }
)
