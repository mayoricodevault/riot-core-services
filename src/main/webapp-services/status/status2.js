
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
	window.location.href = "status2.html?mins=" + mins;
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
        	  count += 1;
        	  
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
	                  
	                  var chart;
	                  
	                  if( bridgeCode == "MCB" )
	        		  {
	                	  chart = Highcharts.charts[0];
	        		  }
	                  else if( bridgeCode == "ALEB" )
	                  {
	                	  chart = Highcharts.charts[1];
	                  }
	                  
	                  if( chart )
	        		  if( propertyName == "lpt" || propertyName == "lpt.avg1" || propertyName == "lpt.avg5" || propertyName == "lpt.avg15"   )
	        		  {
	        			  chart.get( propertyName ).update( [ x, y ], false, true );        			  
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


getChart = function ( titletxt, ylabeltxt, namestxt /*s1name, s2name*/ )
{
	var obj =
	{
			chart: {
	            type: 'gauge',
	            plotBackgroundColor: null,
	            plotBackgroundImage: null,
	            plotBorderWidth: 0,
	            plotShadow: false
	        },

	        title: {
	            text: titletxt
	        },

	        pane: {
	            startAngle: -150,
	            endAngle: 150,
	            background: [{
	                backgroundColor: {
	                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	                    stops: [
	                        [0, '#FFF'],
	                        [1, '#333']
	                    ]
	                },
	                borderWidth: 0,
	                outerRadius: '109%'
	            }, {
	                backgroundColor: {
	                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
	                    stops: [
	                        [0, '#333'],
	                        [1, '#FFF']
	                    ]
	                },
	                borderWidth: 1,
	                outerRadius: '107%'
	            }, {
	                // default background
	            }, {
	                backgroundColor: '#DDD',
	                borderWidth: 0,
	                outerRadius: '105%',
	                innerRadius: '103%'
	            }]
	        },

	        // the value axis
	        yAxis: {
	            min: 0,
	            max: 1200,

	            minorTickInterval: 'auto',
	            minorTickWidth: 1,
	            minorTickLength: 10,
	            minorTickPosition: 'inside',
	            minorTickColor: '#666',

	            tickPixelInterval: 30,
	            tickWidth: 2,
	            tickPosition: 'inside',
	            tickLength: 10,
	            tickColor: '#666',
	            labels: {
	                step: 2,
	                rotation: 'auto'
	            },
	            title: {
	                text: 'milliseconds'
	            },
	            plotBands: [{
	                from: 0,
	                to: 800,
	                color: '#55BF3B' // green
	            }
	            , {
	                from: 800,
	                to: 1000,
	                color: '#DDDF0D' // yellow
	            }
	            , {
	                from: 1000,
	                to: 1200,
	                color: '#DF5353' // red
	            }
	            ]
	        },

//	                dial: 
//	                       {
//	                    radius: '80%',
//	                    backgroundColor: 'silver',
//	                    borderColor: 'black',
//	                    borderWidth: 1,
//	                    baseWidth: 10,
//	                    topWidth: 1,
//	                    baseLength: '90%', // of radius
//	                    rearLength: '20%'
//	                } 
	        
	        series: [{
	            data: [
				{
				    id: 'lpt.avg15',
				    y: 0,
				    dial: {
				        radius: '30%',
				        //backgroundColor: '#0000ff',
				        backgroundColor: '#999999',
				        baseWidth: 13,
				        rearLength: 0
				    }
				},
				{
	                id: 'lpt.avg5',
	                y: 0,
	                dial: {
	                    radius: '50%',
	                    //backgroundColor: '#00ff00',
	                    backgroundColor: '#555555',
	                    baseWidth: 9,
	                    rearLength: 0
	                }
	            }, 
				{
	                id: 'lpt.avg1',
	                y: 0,
	                dial: {
	                	radius: '70%',
	                    //baseLength: '60%',
	                    //backgroundColor: '#ff0000',
	                    backgroundColor: '#333333',
	                    baseWidth: 5,
	                    rearLength: 0
	                }
	            }, 	            
	            {
	                id: 'lpt',
	                y: 0,
	                dial: {
	                    radius: '100%',
	                    baseWidth: 1,
	                    //backgroundColor: '#000000',
	                    backgroundColor: '#000000',
	                    baseLength: '95%',
	                    rearLength: '20%'
	                }
	            }
	            ],
	            animation: false,
	            dataLabels: {
	                enabled: false
	            }
	        }]
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
        
        $('#container').highcharts( getChart( "Loop Processing Time - MCB", "Message Age [ms]", [{name:"MCB"}, {name:"ALEB"}] )  );

        $('#container2').highcharts( getChart( "Loop Processing Time - ALEB", "Message Age [ms]", [{name:"MCB"}, {name:"ALEB"}] ) );
//        
//        $('#container3').highcharts( getChart( "Uptime", "Uptime [ms]", [{name:"APP"}, {name:"MCB"}, {name:"ALEB"}] ) );
//        
//        $('#container4').highcharts( getChart( "Memory Used", "Memory Used [MB]", [{name:"APP"}, {name:"MCB"}, {name:"ALEB"}] ) );
//        
//        $('#container5').highcharts( getChart( "Things per Message", "Things", [{name:"MCB"}, {name:"ALEB"}] ) );
//        
//        $('#container6').highcharts( getChart( "Total Things", "Things", [{name:"MCB"}, {name:"ALEB"}] ) );
//        
//        $('#container7').highcharts( getChart( "Period", "Period [ms]", [{name:"MCB"}, {name:"ALEB"}] ) );
//        
//        $('#container8').highcharts( getChart( "Concurrent Users", "CU", [{name:"APP"}] ) );
//           
//        $('#container9').highcharts( getChart( "New Things / Second", "New Things / Second", [{name:"MCB"}, {name:"ALEB"}] ) );
//        
 
        setTimeout( loadStatus, 1000 );
        
    });
});

setInterval( loadStatus, 10000 );

