//var mins = location.search.split('mins=')[1] ? location.search.split('mins=')[1] : 15;

//alert( 'mins=' + mins );

var xmlhttp;
var loadStatus;

// var maxtime = (new Date()).getTime() - mins * 60 * 1000;
// var maxcount = mins * 6;
// var count = 0;

// var selectMinutes = function( select )
// {
// mins = select.options[select.selectedIndex].value;
// window.location.href = "status.html?mins=" + mins;
// }

function loadFromEndpoint(url, cfunc) {
	if (window.XMLHttpRequest) {
		// code for IE7+, Firefox, Chrome, Opera, Safari
		xmlhttp = new XMLHttpRequest();
	} else {
		// code for IE6, IE5
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	}
	xmlhttp.onreadystatechange = cfunc;
	xmlhttp.open("GET", url, true);
	xmlhttp.setRequestHeader("Api_key", "root");
	xmlhttp.send();
}

loadStatus = function() {
	loadFromEndpoint("/riot-core-services/api/healthAndStatus/loggers",
			function() {
				if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
					var text = xmlhttp.responseText;

					// alert(text);

					var table = document.getElementById("myTable");

					var lines = text.split('\n');

					for ( var i = 0; i < lines.length; i++) {
						var line = lines[i];
						var words = line.split(',');

						if (words.length == 3) {
							var bridgeCode = words[0];
							var loggerName = words[1];
							var loggerLevel = words[2];

							// Create an empty <tr> element and add it to the
							// 1st position of the table:
							var row = table.insertRow(-1);

							// Insert new cells (<td> elements) at the 1st and
							// 2nd position of the "new" <tr> element:
							var cell1 = row.insertCell(0);
							var cell2 = row.insertCell(1);

							var cell3 = row.insertCell(2);
							var cell4 = row.insertCell(3);
							var cell5 = row.insertCell(4);
							var cell6 = row.insertCell(5);
							var cell7 = row.insertCell(6);

							// Add some text to the new cells:
							cell1.innerHTML = bridgeCode;
							cell2.innerHTML = loggerName;

							cell3.innerHTML = "<input type=\"radio\" name=\""
									+ loggerName + "\" value=\"error\" />";
							
							cell4.innerHTML = "<input type=\"radio\" name=\""
									+ loggerName + "\" value=\"warn\" />";
							
							cell5.innerHTML = "<input type=\"radio\" name=\""
									+ loggerName + "\" value=\"info\" selected=\"true\"/>";
							
							cell6.innerHTML = "<input type=\"radio\" name=\""
									+ loggerName + "\" value=\"debug\" />";
							
							cell7.innerHTML = "<input type=\"radio\" name=\""
									+ loggerName + "\" value=\"trace\" />";

						}
					}
				}
			});
}

$(function() {

	$(document).ready(function() {

		// $('#container').highcharts( getChart( "Loop Processing Time", "Loop
		// Processing Time [ms]", [{name:"MCB"}, {name:"ALEB"}] ) );

		setTimeout(loadStatus, 10);

	});
});

// setInterval( loadStatus, 10000 );

