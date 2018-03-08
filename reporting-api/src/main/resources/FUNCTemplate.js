echo 'var funcField = db.system.js.findOne({"_id": <functionName>});
if (funcField.value.constructor === Code) {
    var funcFieldToRun  = eval("(" + funcField.value.code + ")");
}else {
    var funcFieldToRun = funcField.value;
}
printjson(funcFieldToRun(<options>))' | <mongoConnection> <grepFunction> > <fileResultPath>