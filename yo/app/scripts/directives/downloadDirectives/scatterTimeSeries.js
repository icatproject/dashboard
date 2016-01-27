angular.module('dashboardApp').directive('scatterTimeSeries', function(){


	function link(scope,element,attr){		

		scope.$watch('data', function(){

			if(scope.data !== undefined){	
			
				var chart = c3.generate({
					 bindto:element[0],


					 data: {

					 	 x:"x",				 	 
		       			 columns : scope.data,
		       			 type:'spline'
				    },
				    legend: {
				    	show:false
				    },
				    axis: {
				        x: {

				            type: 'timeseries',
				            tick: {
				                format: '%Y-%m-%d'
				            },
				            label: 'Dates'
				        },
				        y: {
				        	min:0,
				        	label: 'MB/S',
				        	padding:{bottom:0}
				        }
				    },
				    tooltip:{
							
							format: {
							    
							    value: function (value, ratio, id) {
							        var format = d3.format('s');
							        return format(value);
							    }

							}
						},
					color:{
						pattern: ['#CF000F','#7f8c8d','#2b2b2b'],
					},
					zoom: {
						enabled:true
					}

				});
			}
		});
}

	return {
			restrict: 'EA',			
			scope: {
				data: '='		

			},
			link : link
		};
});