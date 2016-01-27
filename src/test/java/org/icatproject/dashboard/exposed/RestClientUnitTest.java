/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.dashboard.exposed;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import static junit.framework.Assert.assertTrue;
import org.junit.Test;
import org.icatproject.dashboard.exposed.DownloadSizeProcessor;
import org.json.simple.JSONArray;
import static junit.framework.Assert.assertTrue;




public class RestClientUnitTest {
    
    
    
    @Test
    public void correctlyDistributeDataOverDays() throws ParseException{
        //Setup Dates
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        LocalDate start = LocalDate.of(2015, Month.JANUARY,02);
        LocalDate end = LocalDate.of(2015, Month.JANUARY,11);        
        DownloadSizeProcessor testProcessor = new DownloadSizeProcessor(start,end);
        
        TreeMap<LocalDate,Long> testMap = RestUtility.createPrePopulatedMap(start, end);
        
        
        //Create some data
        List<Object[]> downloadList = new ArrayList<Object[]>();
        
        Object[] sameDayDownload = new Object[3];
        Object[] multipleDayDownload = new Object[3];
        Object[] overflowDownload = new Object[3];
        Object[] earlyDownload = new Object[3];
        
        sameDayDownload[0] = formatter.parse("2015/01/09 15:00:00");
        sameDayDownload[1] = formatter.parse("2015/01/09 16:00:00");
        sameDayDownload[2] = new Long(9999999);
        LocalDate sameDayDate = LocalDate.of(2015, Month.JANUARY,9);
        
        overflowDownload[0] = formatter.parse("2015/01/10 15:00:00");
        overflowDownload[1] = formatter.parse("2015/01/14 15:00:00");
        overflowDownload[2] = new Long(9600);
        LocalDate overFlowDate = LocalDate.of(2015, Month.JANUARY,10);
        
        earlyDownload[0] = formatter.parse("2015/01/01 15:00:00");
        earlyDownload[1] = formatter.parse("2015/01/05 15:00:00");
        earlyDownload[2] = new Long(9600);
        LocalDate earlyDayDate = LocalDate.of(2015, Month.JANUARY,2);
        
        multipleDayDownload[0] = formatter.parse("2015/01/03 15:00:00");
        multipleDayDownload[1] = formatter.parse("2015/01/07 15:00:00");
        multipleDayDownload[2] = new Long(9600);        
        LocalDate initialMultiDayDate = LocalDate.of(2015, Month.JANUARY,03);     
        LocalDate endMultiDayDate = LocalDate.of(2015, Month.JANUARY,05);
        
        //Add the data to the download list (Simulates how data will be returned by the database)
        downloadList.add(earlyDownload);
        downloadList.add(overflowDownload);
        downloadList.add(sameDayDownload);
        downloadList.add(multipleDayDownload);
        
        //Send data to the class we want to test
        testMap = testProcessor.calculateDataDownloaded(downloadList);
        
        long sameDayValue = testMap.get(sameDayDate);
        long initialMultiDayValue = testMap.get(initialMultiDayDate);        
        long endMultiDayValue = testMap.get(endMultiDayDate);
        long overflowDayValue = testMap.get(overFlowDate);
        
        
        
        assertTrue(sameDayValue==9999999 && initialMultiDayValue==3300 && endMultiDayValue==3900 && overflowDayValue == 900 );
        
      
        
        
    }
    
    @Test
    public void dateDifferenceShouldBeNotNull(){
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(3);      
        
        long difference = RestUtility.calculateHourDifference(start, end);
        
        assertTrue(difference == 72);
    }
        
   
    
   
    
}
