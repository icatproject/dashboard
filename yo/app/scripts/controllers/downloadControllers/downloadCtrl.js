(function(){

	'use strict';

	angular.module('dashboardApp').controller('DownloadCtrl', DownloadCtrl);

	DownloadCtrl.$inject= ['DownloadService','$scope'];

	function DownloadCtrl(DownloadService, $scope){	

	
		var self=this;

		$scope.$watch('myDateRange',function(newDate){

			

			var startDate = Date.parse($scope.myDateRange.startDate);
			var endDate = Date.parse($scope.myDateRange.endDate);

			var downloadRoutes = DownloadService.getDownloadRoute(startDate,endDate);
			var downloadCount = DownloadService.getDownloadFrequency(startDate,endDate);
			var downloadBandwidth = DownloadService.getDownloadBandwidth(startDate,endDate);
			var downloadSize = DownloadService.getDownloadSize(startDate,endDate);
			var globalDownloadLocation = DownloadService.getGlobalDownloadLocation(startDate,endDate);

			downloadRoutes.then(function(responseData){	
				  	var data = responseData;			  			  	
				    $scope.downloadRoutes = _.map(data, function(data){
						return [data.method, data.amount];
					});
		    });

		    downloadCount.then(function(responseData){

		    	var data = responseData;

		    	var dates  = _.map(data, function(data){
					return data.date;
				});

				var amounts = _.map(data, function(data){
					return data.amount;
				});

				dates.unshift("x");		
							
				amounts.unshift('Count');	    		
					
			    $scope.downloadCount = [dates,amounts];
					
		    });

		    downloadBandwidth.then(function(responseData){
		    	
		    	$scope.rawBandwidthData = responseData;

		    	var bandwidthData = [];

		    	var dateArray = getDateArray(newDate.startDate,newDate.endDate)
		    	var formattedDateArray = getFormattedDateArray(newDate.startDate,newDate.endDate)

		    	formattedDateArray.unshift("x")	    	
		    	 
		    	bandwidthData.push(formattedDateArray) 

		    	
		    	responseData.forEach( function(download){
		    		var downloadArray = [];		    		

		    		var start = moment(download.startDate, "YYYY-MM-DD HH:mm:ss");
		    		var end = moment(download.endDate , "YYYY-MM-DD HH:mm:ss");

	    			for(var i = 0; i< dateArray.length; i++){
	    				var current = dateArray[i];

	    				if((current.isAfter(start,'day') || current.isSame(start,'day'))&&(current.isBefore(end,'day') || current.isSame(end,'day'))){
	    					
	    					downloadArray.push(download.bandwidth)
	    				}
	    				else{
	    					downloadArray.push("null")
	    				}	    			    
	    			}

	    			downloadArray.unshift(download.id)
	    			   
	    			bandwidthData.push(downloadArray)
		    			
					});
		    	
		    	$scope.downloadBandwidth = bandwidthData;

		    });

			
	    	downloadSize.then(function(responseData){
	    		var data = responseData;

	    		var dates  = _.map(data, function(data){
					return data.date;
				});

				var amounts = _.map(data, function(data){
					return Math.round(data.amount/1000000);
				});

				dates.unshift("x");		
							
				amounts.unshift('Count');				

	    		$scope.downloadSize = [dates,amounts];
	    	});

	    	globalDownloadLocation.then(function(responseData){ 

			  	var data = responseData;	

			    $scope.globalDownloadLocation = _.map(data, function(data){
					return [data.countryCode, data.amount];
				});

	    		
			//Now the calls are done calculate the metrics

			//Count metrics
	    	self.getCountTotal = function(){
	    		var data = $scope.downloadCount;
	    		var total = 0;	    		
	    		
	    		for(var i=1;i<data[1].length;i++){	    			
	    			total +=data[1][i];
	    		}

	    		return total;
	    	}

	    	self.getCountBusiestDay = function(){
	    		var data = $scope.downloadCount;

	    		var busiestIndex = 0;
	    		var largestDay = 0;	    		
	    		
	    		for(var i=1;i<data[1].length;i++){	    			
	    			if(data[1][i] > largestDay){
	    				busiestIndex = i;
	    				largestDay = data[1][i];
	    			}
	    		}

	    		return data[0][busiestIndex] +" with "+largestDay;
	    	}

	    	//Badnwidth metrics
	    	self.getHeighestBandwidth = function(){	    		

		    	return getBandwidthMetrics($scope.rawBandwidthData)[1]+" MB/S";	

	    	}

	    	self.getLowestBandwidth = function(){
	    		
	    		return getBandwidthMetrics($scope.rawBandwidthData)[0]+" MB/S";
	    	}

	    	//Download amount metrics

	    	self.getTotalAmount = function(){
	    		var data = $scope.downloadSize;
	    		var total = 0;	    		
	    		
	    		for(var i=1;i<data[1].length;i++){	    			
	    			total +=data[1][i];
	    		}

	    		return total +" Megabytes";
	    	}


	    	

	    	self.getBusiestDayAmount = function(){
	    		var data = $scope.downloadSize;

	    		var busiestIndex = 0;
	    		var largestDay = 0;	    		
	    		
	    		for(var i=1;i<data[1].length;i++){	    			
	    			if(data[1][i] > largestDay){
	    				busiestIndex = i;
	    				largestDay = data[1][i];
	    			}
	    		}

	    		return data[0][busiestIndex] +" with "+largestDay +" Megabytes";

	    	}

	    	

	    });

	    	


	    	
	    });

		
		
	}

	function getBandwidthMetrics(data) {     		

    		var highest = 0;
    		var lowest = 9999999;
    		
	    	data.forEach( function(download){
	    		if(download.bandwidth >highest){
	    			highest = download.bandwidth;
	    		}
	    		if(download.bandwidth < lowest && download.bandwidth !=='0.0'){
	    			lowest = download.bandwidth;
	    		}
	    	});

	    	return[lowest,highest]
	}

	function getDateArray(startDate,stopDate) {
		var dateArray = [];
	    var currentDate = moment(startDate);
	    while (currentDate.isBefore(stopDate) || currentDate.isSame(stopDate)) {
	        dateArray.push( moment(currentDate) )
	        currentDate = moment(currentDate).add(1, 'days');
	    }
	    return dateArray;
	}

	function getFormattedDateArray(startDate,stopDate){
		var dateArray = [];
	    var currentDate = moment(startDate);
	    while (currentDate.isBefore(stopDate) || currentDate.isSame(stopDate)) {
	        dateArray.push( moment(currentDate).format("YYYY-MM-DD") )
	        currentDate = moment(currentDate).add(1, 'days');
	    }
	    return dateArray;

	}

	function mapJsonDataToArry(data){		
		    		
		var dates  = _.map(data, function(data){
			return data.date;
		});

		var amounts = _.map(data, function(data){
			return data.amount;
		});

		dates.unshift("x");		
					
		amounts.unshift('Count');

		var result = [dates,amounts];

		return result;

	}






})();