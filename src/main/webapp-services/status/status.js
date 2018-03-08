
var mins = location.search.split('mins=')[1] ? location.search.split('mins=')[1] : 15;

//alert( 'mins=' + mins );

var xmlhttp;
var loadStatus;

var maxtime = (new Date()).getTime() - mins * 60 * 1000;
var maxcount = mins * 6;
var count = 0;

var selectMinutes = function( select )
{
	mins = select.options[select.selectedIndex].value;
	window.location.href = "status.html?mins=" + mins;
}

function loadFromEndpoint( url, cfunc )
{
	if( window.XMLHttpRequest )
	{
		// code for IE7+, Firefox, Chrome, Opera, Safari
		xmlhttp = new XMLHttpRequest();
	}
	else
	{
		// code for IE6, IE5
		xmlhttp = new ActiveXObject( "Microsoft.XMLHTTP" );
	}
	xmlhttp.onreadystatechange = cfunc;
	xmlhttp.open( "GET", url + maxtime, true );
	xmlhttp.setRequestHeader( "Api_key", "root" );
	xmlhttp.send();
}

loadStatus = function ()
{
	loadFromEndpoint( "/riot-core-services/api/healthAndStatus/?time=", function()
      {
          if( xmlhttp.readyState == 4 && xmlhttp.status == 200 )
            {
        	  var text = xmlhttp.responseText;
        	  var lines = text.split( '\n' );
        	  
        	  for( var i = 0; i < lines.length; i++ )
        	  {
        		  var line = lines[i];
        		  var words = line.split( ',' );
        		  
        		  if( words.length == 4 )
        		  {
	        		  var bridgeCode = words[0];
	        		  var propertyName = words[1];
	        		  var x = parseInt( words[2] );       
	                  var y = parseInt( words[3] );
	                  
	                  if( x > maxtime )
	                  {
	                	  maxtime = x;
	                  }
	                   
	        		  if( propertyName == "lpt" )
	        		  {
	        			  var chart = Highcharts.charts[0];
	        			  if( bridgeCode == "MCB" )
		        		  {
	        				  count += 1;
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "age" )
	        		  {
	        			  var chart = Highcharts.charts[1];
	        			  if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "uptime" )
	        		  {
	        			  var chart = Highcharts.charts[2];
	        			  if( bridgeCode == "APP" )
		        		  {
		        			  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[2].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "mem.used" )
	        		  {
	        			  var chart = Highcharts.charts[3];
	        			  if( bridgeCode == "APP" )
		        		  {
		        			  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[2].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "thing_count.avg1" )
	        		  {
	        			  var chart = Highcharts.charts[4];
	        			  if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "thing_count_total" )
	        		  {
	        			  var chart = Highcharts.charts[5];
	        			  if( bridgeCode == "APP" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[2].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "period.avg1" )
	        		  {
	        			  var chart = Highcharts.charts[6];
	        			  if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "concurrentUsers" )
	        		  {
	        			  var chart = Highcharts.charts[7];
	        			  if( bridgeCode == "APP" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "new_things.avg1" )
	        		  {
	        			  var chart = Highcharts.charts[8];
	        			  if( bridgeCode == "MCB" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        			  else if( bridgeCode == "ALEB" )
		        		  {
		        			  chart.series[1].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }

	        		  else if( propertyName == "cpu.processLoad" )
	        		  {
	        		  	  var chart = Highcharts.charts[9];	
	        		  	  if( bridgeCode == "APP" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
	        		  
	        		  else if( propertyName == "endpointAssociateCount" )
	        		  {
	        		  	  var chart = Highcharts.charts[10];	
	        		  	  if( bridgeCode == "APP" )
		        		  {
	        				  chart.series[0].addPoint( [ x, y ], false, count > maxcount ); 
		        		  }
	        		  }
        		  }
        	  }
        	  
        	  for( var i = 0; i < Highcharts.charts.length; i++ )
        	  {
        		  Highcharts.charts[i].redraw();
        	  }
        	  
        	  //document.getElementById( "myDiv" ).innerHTML = string;
            }   		
      });
}


getChart = function ( title, ylabel, names /*s1name, s2name*/ )
{
	var obj =
	{
		chart: {
            type: 'line',
            animation: Highcharts.svg, 
            marginRight: 10,
            events: {
                load: function () {
                	
                }
            }
        },
        title: {
            text: title
        },
        xAxis: {
            type: 'datetime',
            tickPixelInterval: 200
        },
        yAxis: {
            title: {
                text: ylabel
            },
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        tooltip: {
            formatter: function () {
                return '<b>' + this.series.name + '</b><br/>' 
                + Highcharts.dateFormat( '%Y-%m-%d %H:%M:%S', this.x ) + '<br/>' 
                + Highcharts.numberFormat( this.y, 2 );
            }
        },
        legend: {
            enabled: true
        },
        exporting: {
            enabled: false
        },
        //series: [{
       //     name: s1name,
       // }, {
       //     name: s2name,
       // }]
        series: names
	};
	
	return obj;
}

$(function () {
	
    $(document).ready(function () {
    	
        Highcharts.setOptions({
            global: {
                useUTC: false
            }
        
        });
        
        $('#container').highcharts( getChart( "Loop Processing Time", "Loop Processing Time [ms]", [{name:"MCB"}, {name:"ALEB"}] ) );

        $('#container2').highcharts( getChart( "Message Age", "Message Age [ms]", [{name:"MCB"}, {name:"ALEB"}] ) );
        
        $('#container3').highcharts( getChart( "Uptime", "Uptime [ms]", [{name:"APP"}, {name:"MCB"}, {name:"ALEB"}] ) );
        
        $('#container4').highcharts( getChart( "Memory Used", "Memory Used [MB]", [{name:"APP"}, {name:"MCB"}, {name:"ALEB"}] ) );
        
        $('#container5').highcharts( getChart( "Things per Message", "Things", [{name:"MCB"}, {name:"ALEB"}] ) );
        
        $('#container6').highcharts( getChart( "Total Things", "Things", [{name:"APP"},{name:"MCB"}, {name:"ALEB"}] ) );
        
        $('#container7').highcharts( getChart( "Period", "Period [ms]", [{name:"MCB"}, {name:"ALEB"}] ) );
        
        $('#container8').highcharts( getChart( "Concurrent Users", "CU", [{name:"APP"}] ) );
           
        $('#container9').highcharts( getChart( "New Things / Second", "New Things / Second", [{name:"MCB"}, {name:"ALEB"}] ) );

        $('#container10').highcharts( getChart( "CPU Usage", "CPU Usage", [{name:"APP"}] ) );
        
        $('#container11').highcharts( getChart( "Associate Endpoint Usage Count (Sharaf Only)", "Count", [{name:"APP"}] ) );
 
        setTimeout( loadStatus, 1000 );
        
    });
});

setInterval( loadStatus, 10000 );

