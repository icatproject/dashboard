(function() {
    'use strict';

    var app = angular.module('dashboardApp');



    app.directive('stackedBar', function(){
    	return {
			restrict: 'EA',			
			scope: {
				data: '=',	
				selectOptions:'&',				

			},

			templateUrl : 'views/graphTemplate.html',

			link:function($scope,$element,$rootScope){
				
	
				var chart;
				
				var divElement = $element.find('.graphAnchor');

				$scope.reset = function(){					
					chart.unzoom();
				}

				
				

				$scope.$watch('data', function(graphObject){
					if(graphObject){
					
						$scope.description = graphObject.description; 
						$scope.title = graphObject.title; 
						$scope.selectOp = graphObject.selectOp;
						$scope.selectTitle = graphObject.optionTitle;
						
					
						chart = c3.generate({
							bindto:divElement[0],

							data:graphObject.data,
						    legend: {
						    	show:false,
						    },
						    axis: {
						    	x:{
						    		type:'category',
						    		categories:graphObject.categories,
						            label: {
						            	text: graphObject.xLabel,
						        		position: 'outer-center',
						            },
						            
						            
						        },
						        y: {
						        	label: {
						        			text: graphObject.yLabel,
						        			position: 'outer-center',
						        		},				        
						    },
						},
						    tooltip:{
									
								},
							color:{
								pattern: ["#2980B9","#4B77BE","#3498DB"],
							},		
						});					
							}
						});


							}
							
						};

				});


	
})();



