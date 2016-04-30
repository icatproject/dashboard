/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.dashboard.exposed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.icatproject.dashboard.consumers.GeoTool;
import org.icatproject.dashboard.entity.EntityBaseBean;
import org.icatproject.dashboard.entity.EntityCount;
import org.icatproject.dashboard.entity.GeoLocation;
import org.icatproject.dashboard.entity.ICATLog;
import org.icatproject.dashboard.entity.InstrumentMetaData;
import org.icatproject.dashboard.entity.InvestigationMetaData;
import org.icatproject.dashboard.exceptions.AuthenticationException;
import org.icatproject.dashboard.exceptions.BadRequestException;
import org.icatproject.dashboard.exceptions.DashboardException;
import org.icatproject.dashboard.exceptions.InternalException;
import static org.icatproject.dashboard.exposed.RestUtility.convertToLocalDateTime;
import org.icatproject.dashboard.manager.EntityBeanManager;
import org.icatproject.dashboard.manager.IcatDataManager;
import org.icatproject.dashboard.manager.PropsManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@Stateless
@LocalBean
@Path("icat")
public class IcatResource {

    @EJB
    EntityBeanManager beanManager;

    @EJB
    PropsManager properties;
    
    @EJB
    IcatDataManager icatData;

    @PersistenceContext(unitName = "dashboard")
    private EntityManager manager;

    
    /**
     * Access the IcatDataManager to get the authenticators mnemonic;
     * @return a JSONArray containing the ICATs mnemonics
     * @throws InternalException issue accessing the IcatDataManager;
     */
    @GET
    @Path("authenticators")
    @Produces(MediaType.APPLICATION_JSON)
    public String getICATAuthenticators() throws InternalException {       

        return icatData.getAuthenticators();
    }

    /**
     * Retrieves the ICAT logs from that are stored in the ICATLog table.
     *
     * @param sessionID for authentication
     * @param queryConstraint the JPQL where query.
     * @param initialLimit the initial value of a limit by expression
     * @param maxLimit the max limit of a limit by expression.
     * @return a JSON array of ICAT Log JSON Objects.
     * @throws DashboardException Troubles accessing the database.
     */
    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    public String getIcatLogs(@QueryParam("sessionID") String sessionID,
            @QueryParam("queryConstraint") String queryConstraint,
            @QueryParam("initialLimit") int initialLimit,
            @QueryParam("maxLimit") int maxLimit) throws DashboardException {

        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }

        String query = "SELECT log, user.fullName from ICATLog log JOIN log.user user ";

        //Check status of passed paramaters and build query.		
        if (!("".equals(queryConstraint))) {
            query += queryConstraint;
        }

        List<Object[]> logs = manager.createQuery(query).setFirstResult(initialLimit).setMaxResults(maxLimit).getResultList();

        JSONArray result = new JSONArray();

        for (Object[] log : logs) {
            JSONObject obj = new JSONObject();
            ICATLog tempLog = (ICATLog) log[0];
            obj.put("fullName", log[1]);
            obj.put("id", tempLog.getId());
            obj.put("entityId", tempLog.getEntityId());
            obj.put("entityType", tempLog.getEntityType());
            obj.put("ipAddress", tempLog.getIpAddress());
            obj.put("logTime", convertToLocalDateTime(tempLog.getLogTime()).toString());
            obj.put("operation", tempLog.getOperation());
            obj.put("query", tempLog.getQuery());
            obj.put("duration", tempLog.getDuration());
            result.add(obj);

        }

        return result.toJSONString();
    }

    /**
     * Retrieves the geoLocation of an ICAT log. If the log was of a functional
     * account then the location will be retrieved with the geo tool as
     * functional account locations aren't stored.
     *
     * @param sessionID for authentication
     * @param logId the unique identifier of an ICAT log.
     * @return a JSON containing the city, longitude and latitude.
     * @throws DashboardException
     */
    @GET
    @Path("logs/location")
    @Produces(MediaType.APPLICATION_JSON)
    public String getIcatLogLocation(@QueryParam("sessionID") String sessionID,
                                     @QueryParam("logId") int logId) throws DashboardException {

        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }

        GeoLocation geoLocation = getLogLocation(logId);

        JSONArray resultArray = new JSONArray();

        JSONObject obj = new JSONObject();
        obj.put("number", 1);
        obj.put("city", geoLocation.getCity());
        obj.put("longitude", geoLocation.getLongitude());
        obj.put("latitude", geoLocation.getLatitude());
        obj.put("countryCode", geoLocation.getCountryCode());
        obj.put("isp", geoLocation.getIsp());
        resultArray.add(obj);

        return resultArray.toJSONString();
    }
    
    /***
     * Retrieves the name of instruments in the ICAT
     * @return A JSONArray of JSONObjects {name:"AlF"}.
     */
    @GET
    @Path("instrument/names")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstrumentNames(){       
        
        JSONArray instrumentNames = new JSONArray();
        
        LinkedHashMap<String, Long> instrumentIds = icatData.getInstrumentIdMapping();
        
        for(String value : instrumentIds.keySet()){
            JSONObject obj = new JSONObject();
            obj.put("name", value);
            instrumentNames.add(obj);
        }        
        
        
        return instrumentNames.toJSONString();
    }
    
    /**
     * Gets the number of entities inserted into the ICAT for each day between the start 
     * and end date.
     * @param entity to search for
     * @param sessionID for authentication.
     * @param startDate to search from.
     * @param endDate to search to.
     * @return a JSONArray containing JSONObjects of {date:2015-01-20, number:200}
     * @throws BadRequestException
     * @throws AuthenticationException 
     */
    @GET
    @Path("{entity}/number")
    public String getEntityCount(@PathParam("entity") String entity,
                                 @QueryParam("sessionID") String sessionID,
                                 @QueryParam("startDate") String startDate,
                                 @QueryParam("endDate") String endDate) throws BadRequestException, AuthenticationException{
        
        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }      
        

        return getEntityCountData(startDate,endDate,entity);
        
        
        
    }
    
    /**
     * Gets a list of entities that have been counted from the ICAT.
     * @return A JSONArray of entity objects e.g. {name:"DATAFILE"}.
     */
    @GET
    @Path("entity/name")
    public String getEntities(){
        
        String entityQuery = "SELECT DISTINCT(entity.entityType) FROM EntityCount as entity ORDER BY entity.entityType ASC";
        

        List<Object> entities = manager.createQuery(entityQuery).getResultList();
        
        JSONArray entityArray = new JSONArray();
        
        for(Object entity : entities){
            JSONObject obj = new JSONObject();
            obj.put("name", entity);
            entityArray.add(obj);
        }
        
        return entityArray.toJSONString();

        
    }
    
    @GET
    @Path("investigation/datafile/volume")
    public String getInvestigationDatafileVolume(
                                 @QueryParam("sessionID") String sessionID,
                                 @QueryParam("startDate") String startDate,
                                 @QueryParam("endDate") String endDate,
                                 @QueryParam("limit")int limit) throws BadRequestException, AuthenticationException{
        
        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }      
        
                
        return getInvestigationMetaData("datafileVolume",startDate,endDate,limit); 
    }
        
    @GET
    @Path("investigation/datafile/number")
    public String getInvestigationDatafileNumber(
                                 @QueryParam("sessionID") String sessionID,
                                 @QueryParam("startDate") String startDate,
                                 @QueryParam("endDate") String endDate,
                                 @QueryParam("limit")int limit) throws BadRequestException, AuthenticationException{
        
        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }      
        
                
        return getInvestigationMetaData("datafileCount",startDate,endDate,limit); 
    }
    
    
    

    /***
     * Gets the number of datafiles created for the specified instrument over a specific date. 
     * @param sessionID for authentication.
     * @param instrument name of the instrument to search for.
     * @param startDate of when the datafiles were created.
     * @param endDate of when the datafiles were created.
     * @return a JSON Array of JSON Objects containing date and number of datafiles.
     * @throws DashboardException 
     */
    @GET
    @Path("{instrument}/datafile/number")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstrumentDataFileCount(@QueryParam("sessionID") String sessionID,
                                    @PathParam("instrument")final String instrument,
                                    @QueryParam("startDate") String startDate,
                                    @QueryParam("endDate") String endDate) throws DashboardException {

        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }
        
        return getInstrumentMetaData("datafileCount", startDate, endDate, instrument);
    }
    
    @GET
    @Path("{instrument}/datafile/volume")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstrumentDataFileVolume(@QueryParam("sessionID") String sessionID,
                                    @PathParam("instrument")final String instrument,
                                    @QueryParam("startDate") String startDate,
                                    @QueryParam("endDate") String endDate) throws DashboardException {

        if (sessionID == null) {
            throw new BadRequestException("sessionID must be provided");
        }
        if (!(beanManager.checkSessionID(sessionID, manager))) {
            throw new AuthenticationException("An invalid sessionID has been provided");
        }
        
      
        
        return getInstrumentMetaData("datafileVolume", startDate, endDate, instrument);
    }
    /**
     * Gets the geolocation of an ICATLog.
     *
     * @param logId the id of the log.
     * @return A geoLocation object of where the Log took place.
     */
    private GeoLocation getLogLocation(int logId) {

        String locationQuery = "SELECT location from GeoLocation location JOIN location.logs log WHERE log.id='" + logId + "'";
        String ipQuery = "SELECT log.ipAddress FROM ICATLog log WHERE log.id='" + logId + "'";

        List<Object> location = manager.createQuery(locationQuery).getResultList();
        GeoLocation geoLocation;

        /*Location has not been set due to it being a functional account log. We do not store that to prevent
             * the geoLocation API blocking the dashboards ip.
         */
        if (location.isEmpty()) {
            List<Object> ipList = manager.createQuery(ipQuery).getResultList();
            geoLocation = GeoTool.getGeoLocation((String) ipList.get(0), manager, beanManager);

        } else {
            geoLocation = (GeoLocation) location.get(0);
        }

        return geoLocation;
    }

    private TreeMap<LocalDate,Long> createDateMap(String startDate,String endDate){
        
        LocalDate startRange = Instant.ofEpochMilli(Long.valueOf(startDate)).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endRange = Instant.ofEpochMilli(Long.valueOf(endDate)).atZone(ZoneId.systemDefault()).toLocalDate();
        
        TreeMap<LocalDate, Long> dateMap = RestUtility.createPrePopulatedMap(startRange, endRange);
        
        return dateMap;
    }
    
    private String convertResultsToJson(List<Object[]> result, TreeMap<LocalDate,Long> dateMap){
        
        for(Object[] day : result){
            LocalDate collectionDate = RestUtility.convertToLocalDate((Date) day[0]);
            
            dateMap.put(collectionDate, (Long) day[1]);
        }     

        return RestUtility.convertMapToJSON(dateMap).toJSONString();
    }
    
    private String getEntityCountData(String startDate, String endDate, String entityType){
        
        TreeMap<LocalDate,Long> dateMap = createDateMap(startDate,endDate);
        
        Date start = new Date(Long.valueOf(startDate));
        Date end = new Date(Long.valueOf(endDate));       
      
        
        //Criteria objects.
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<EntityCount> entityCount = query.from(EntityCount.class);        
        
        Predicate startGreater = cb.greaterThanOrEqualTo(entityCount.<Date>get("countDate"), start);
        Predicate endLess = cb.lessThanOrEqualTo(entityCount.<Date>get("countDate"), end); 
        Predicate entity = cb.equal(entityCount.<Long>get("entityType"), entityType);
        
        Predicate dateRange = cb.and(startGreater, endLess); 
        
        Predicate finalPredicate = cb.and(dateRange,entity);       
       
        query.multiselect(entityCount.<Date>get("countDate"), entityCount.<Long>get("entityCount"));        
        
        query.where(finalPredicate);     
       
        
        List<Object[]> result = manager.createQuery(query).getResultList();
        
        return convertResultsToJson(result,dateMap);
        
    }
   
    private String getInvestigationMetaData(String type, String startDate, String endDate, int limit){
        
        Date start = new Date(Long.valueOf(startDate));
        Date end = new Date(Long.valueOf(endDate));
        
        
        //Criteria objects.
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<InvestigationMetaData> investigationMeta = query.from(InvestigationMetaData.class);        
        
        Predicate startGreater = cb.greaterThanOrEqualTo(investigationMeta.<Date>get("collectionDate"), start);
        Predicate endLess = cb.lessThanOrEqualTo(investigationMeta.<Date>get("collectionDate"), end);         
        
        Predicate dateRange = cb.and(startGreater, endLess);   
        
        
        query.multiselect(investigationMeta.<Long>get("investigationId"), cb.sum(investigationMeta.<Long>get(type))); 
        
        
        query.where(dateRange);
        query.groupBy(investigationMeta.get("investigationId"));
        
        query.orderBy(cb.desc(cb.sum(investigationMeta.<Long>get(type))));
        
        List<Object[]> result = manager.createQuery(query).setFirstResult(0).setMaxResults(limit).getResultList();
        
        JSONArray resultArray =  new JSONArray();
        
        for(Object[] investigation: result){
            JSONObject obj = new JSONObject();
            obj.put("investigationId",investigation[0]);
            obj.put("value",investigation[1]);
            resultArray.add(obj);
            
        }
        
        return resultArray.toJSONString();
    }
    
    private String getInstrumentMetaData(String type, String startDate, String endDate, String instrument){
        
        TreeMap<LocalDate,Long> dateMap = createDateMap(startDate,endDate);
        
        Date start = new Date(Long.valueOf(startDate));
        Date end = new Date(Long.valueOf(endDate));
        
        Long instrumentId = icatData.getInstrumentIdMapping().get(instrument);
        
        //Criteria objects.
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<InstrumentMetaData> instrumentMeta = query.from(InstrumentMetaData.class);        
        
        Predicate startGreater = cb.greaterThanOrEqualTo(instrumentMeta.<Date>get("collectionDate"), start);
        Predicate endLess = cb.lessThanOrEqualTo(instrumentMeta.<Date>get("collectionDate"), end); 
        Predicate instrumentName = cb.equal(instrumentMeta.<Long>get("instrumentId"), instrumentId);
        
        Predicate dateRange = cb.and(startGreater, endLess); 
        
        Predicate finalPredicate = cb.and(dateRange,instrumentName);       
       
        query.multiselect(instrumentMeta.<Date>get("collectionDate"), instrumentMeta.<Long>get(type));  
        
        
        query.where(finalPredicate);
        
       
        
        List<Object[]> result = manager.createQuery(query).getResultList();
        
        return convertResultsToJson(result,dateMap);
        
    }

}
