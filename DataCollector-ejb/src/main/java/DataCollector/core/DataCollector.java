/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DataCollector.core;



import entityManager.core.manager.DashboardException;
import entityManager.core.manager.EntityBeanManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.namespace.QName;
import org.apache.log4j.Logger;
import org.icatproject.*;
import org.icatproject.Login.Credentials;
import org.icatproject.Login.Credentials.Entry;






@Singleton
@Lock(LockType.READ)
@Startup
public class DataCollector {
    
    
    @PersistenceContext(unitName="Dashboard")
    private EntityManager manager;
        
    @EJB
    private EntityBeanManager entityManager;
    
    private static final Logger log = Logger.getLogger(DataCollector.class);
   
    private PropsManager properties;
    private String sessionID;
    private ICAT icat;
    
    
            
    @Resource
    private TimerService timerService;
    
    /**
     * Init method is called once the EJB has been loaded. Does the initial property
     * collections and login into ICAT. Also initiates initial data collection.
     */
    @PostConstruct
    private void init() { 
        
         log.info("Reading properties.");
         properties = new PropsManager("dashboard.properties");
         log.info("Logging into ICAT");
         sessionID = loginICAT(properties.getICATUrl(),properties.getReaderUserName(),properties.getReaderPassword(),properties.getAuthenticator());
         createTimers(properties);
         
       
        
        try {
            String testSession = entityManager.login("yqa41233",60,manager);
        } catch (DashboardException ex) {
            java.util.logging.Logger.getLogger(DataCollector.class.getName()).log(Level.SEVERE, null, ex);
        }
         
                
        
        
        }

    /**
     * Creates the timers with the TimerService object. Currently creates two.
     * One for the ICAT refresh with is invoked every hour and datacollect which
     * is invoked depending on the users set value.
     * 
     * @param properties The properties manager object that contains all the information
     * from the dashboard.properties file.
     */
    private void createTimers(PropsManager properties){
        
        TimerConfig refreshSession = new TimerConfig("refreshSession", false);
        timerService.createIntervalTimer(3600000,3600000,refreshSession); 
        
        TimerConfig dataCollect = new TimerConfig("dataCollect",false);
        timerService.createCalendarTimer(new ScheduleExpression().hour(properties.getCollectTime()),dataCollect);
        
    }
   
    /**
     * Handles the timers. If statement inside decides what timer was called and what
     * method should be invoked to deal with that timer.
     * Currently only refresh session and collection of data.
     * @param timer is the object that is invoked when the timerservice is invoked.
     */
    @Timeout
    public void timeout(Timer timer){
        if("refreshSession".equals(timer.getInfo())){
            try {
                refreshSession();
            } catch (IcatException_Exception ex) {
                java.util.logging.Logger.getLogger(DataCollector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
   /**
    * Refreshes the ICAT session ID so the collector can keep connected to the ICAT
    * @throws IcatException_Exception Incase it can't connect to the ICAT
    */
    private void refreshSession() throws IcatException_Exception{
        log.info("Refresshing the Session ID");
        icat.refresh(sessionID);
    }
    
    /*
    Logins into the ICAT and returns the sessionID.
    */
    private String loginICAT(String icatURL, String userName, String password, String authenticator){
        
        
        try {
            URL hostUrl;
            
            hostUrl = new URL("https://"+icatURL);
            URL icatUrl = new URL(hostUrl, "ICATService/ICAT?wsdl");
            QName qName = new QName("http://icatproject.org", "ICATService");
            ICATService service = new ICATService(icatUrl, qName);
            icat = service.getICATPort();            
                                        
                    
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(DataCollector.class.getName()).log(Level.SEVERE, null, ex);
        }
        
       
        
        try {
            sessionID = icat.login(authenticator, getCredentials(userName,password));
            log.info("Successfully Logged into ICAT");
        } catch (IcatException_Exception ex) {
            java.util.logging.Logger.getLogger(DataCollector.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return sessionID;
        
    }
    
    /**
     * Puts the username and password into a Credentials object.
     * @param userName for the reader account.
     * @param password for the reader account.
     * @return credentials object containing username and password.
     */
    public Credentials getCredentials(String userName, String password){
        
        Credentials credentials = new Credentials(); 
        List<Entry> entries = credentials.getEntry(); 
        Entry e; 

        e = new Entry(); 
        e.setKey("username"); 
        e.setValue(userName); 
        entries.add(e); 
        e = new Entry(); 
        e.setKey("password"); 
        e.setValue(password); 
        entries.add(e);
        
        return credentials;
        
    }    
   
    

}
