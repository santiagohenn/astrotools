package astrotools.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.hipparchus.ode.events.Action;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.*;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import astrotools.structures.LLA;
import astrotools.structures.OrbitalData;
import simulation.Facility;
import simulation.Satellite;
import simulation.Scenario;

public class Simulation implements Runnable{
	
	public int intervalCount = 0;
	public int gapCount = 0;
	public ArrayList<long[]> intervals = new ArrayList<long[]>();
	public String time1 = "2020-03-20T11:00:00.000";
	public String time2 = "2020-03-30T11:00:00.000";
	
	public Scenario scenario;
	public Satellite satellite = new Satellite();
	public Facility fac = new Facility();
	
	public OrbitalData sat;
	public LLA gs;
	
	public double step;
	public double th;
	public Frame earthFrame;
	public  BodyShape earth;
	private double latitude;
	private double longitude;
	private double altitude;
	public GeodeticPoint station;
	public TopocentricFrame topoFrame;
	public TLE tle;
	public TLEPropagator tlePropagator;
	public EventDetector elevDetector;
	double thDetection = 0.001; // 1 ms default
	public Date contact = new Date();
	public File orekitData;
	public DataProvidersManager manager;
	public double totalAccess = 0;
	public double lastSimTime = 0;

	public Simulation(){
		init();
	}
	
	public Simulation(Scenario sc){
		this.scenario = sc;
		this.time1 = sc.getStart();
		this.time2 = sc.getEnd();
		this.step = sc.getStep();
		init();
	}
	
	public Simulation(String time1, String time2, double step, double th){
		
		init();
    	this.time1 = time1;
    	this.time2 = time2;
    	this.step = step;
    	this.th = th;
    	
	}

	public void setScenario(Scenario sc){
		this.scenario = sc;
		this.time1 = sc.getStart();
		this.time2 = sc.getEnd();
		this.step = sc.getStep();
	}
	/*
    public void setFac(LLA gs){
    	this.gs = gs;
		this.latitude = Math.toRadians(gs.getLatitude());
		this.longitude = Math.toRadians(gs.getLongitude());
		this.altitude = gs.getAltitude();
		this.station = new GeodeticPoint(latitude, longitude, altitude);
		this.topoFrame = new TopocentricFrame(earth, station, gs.getId());
    }

    public void setSat(OrbitalData sat){
		this.sat = sat;
		if (sat.getTLE1().isEmpty() || sat.getTLE2().isEmpty()){
			this.tle = elements2tle(sat.getElements(),time1);}
		else {
			this.tle = new TLE(sat.getTLE1(),sat.getTLE2());
		}
		this.tlePropagator = TLEPropagator.selectExtrapolator(tle);
    }
    */
    private void init(){
    	
    	// configure Orekit
        orekitData = new File("orekit-data");
        if (!orekitData.exists()) {
            System.err.format(Locale.US, "Failed to find %s folder%n",
                              orekitData.getAbsolutePath());
            System.exit(1);
        }
        
        manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        
        // configure Earth frame:
        this.earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        this.earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     earthFrame);

    }
    
    public ArrayList<long[]> getAccessIntervals(){
    	return this.intervals;
    }
    
    public double getTotalAccess(){
    	double sum = 0;
        for (int j=0; j<intervalCount; j++){
        	sum = sum + (intervals.get(j)[1] - intervals.get(j)[0]);
        }
        System.out.println("Total access time: " + totalAccess);
        return sum;
    }

    public void computeAccessBetween(Facility from, Satellite to){
    	computeAccessBetween(from.getId(),to.getId());
    }
    
    public void computeAccessBetween(String idFrom, String idTo){
    	if (fac.getId() != idFrom) setFacility(idFrom);
    	if (satellite.getId() != idFrom) setSatellite(idTo);
    	computeAccess();
    }
    
    public void setFacility(String id){
    	this.fac = scenario.getFacility(id);
    	this.th = fac.getTHrad();
		this.station = new GeodeticPoint(fac.getLatRad(), fac.getLonRad(), fac.getAlt());
		this.topoFrame = new TopocentricFrame(earth, station, fac.getId());
    }
    
    public void setSatellite(String id){
    	this.satellite = scenario.getSat(id);
		if (satellite.getTLE1().isEmpty() || satellite.getTLE2().isEmpty()){
			this.tle = elements2tle(satellite.getElements(),time1);}
		else {
			this.tle = new TLE(satellite.getTLE1(),satellite.getTLE2());
		}
		this.tlePropagator = TLEPropagator.selectExtrapolator(tle);
    }
    
    public void computeAccess(){
    	
    	contact.setTime(Transformations.stamp2unix(time1));
    	intervals.clear();
    	intervalCount = 0;
    	
        this.elevDetector =
                new ElevationDetector(step, thDetection, topoFrame).
                withConstantElevation(th).
                withHandler(
                (s, detector, increasing) -> {
                addInterval(s, detector, increasing);
                return Action.CONTINUE;
                });
        
        this.tlePropagator.addEventDetector(elevDetector);
        
        AbsoluteDate ad1 = stamp2AD(time1);
        AbsoluteDate ad2 = stamp2AD(time2);
        long t0 = System.currentTimeMillis();
    	accessBetweenDates(ad1,ad2);
    	lastSimTime = System.currentTimeMillis()-t0;
    	
    }

    private void accessBetweenDates(AbsoluteDate time1, AbsoluteDate time2){
   		double scenarioTime = time2.durationFrom(time1);
   		tlePropagator.propagate(time1,time1.shiftedBy(scenarioTime));
   	}  	

    private AbsoluteDate stamp2AD(String stamp){
    	return new AbsoluteDate(stamp, TimeScalesFactory.getUTC());
    }

    private void addInterval(SpacecraftState s, ElevationDetector detector, boolean dir){

    	try {
		if(dir){
			contact = s.getDate().toDate(TimeScalesFactory.getUTC());
			}
		else{
			long[] last = {contact.getTime(), s.getDate().toDate(TimeScalesFactory.getUTC()).getTime()};
			intervals.add(intervalCount, last);
	        intervalCount++;
		}}
    	catch (NullPointerException npe){
    		npe.printStackTrace();
    		System.out.println("Why 1: " + contact.toString());
    		//System.out.println("Why 2: " + s.getDate().toDate(TimeScalesFactory.getUTC()).getTime());
    		
    	}

    }

    public TLE elements2tle(double[] elements, String time){
    	
		TLE tle = new TLE(0, 'U', Integer.parseInt(time.substring(0, 4)), 218, "A", 0, 0, stamp2AD(time), Transformations.getMeanMotion(elements[0]), 
		0, 0, elements[1], Math.toRadians(elements[2]), Math.toRadians(elements[3]), Math.toRadians(elements[4]), Math.toRadians(elements[5]), 7836, 0.11873e-3);
		return tle;
    }
    
    public void printSatInfo(double elements[], String time){
    	
		TLE tle = new TLE(0, 'U', Integer.parseInt(time.substring(0, 4)), 218, "A", 0, 0, stamp2AD(time), Transformations.getMeanMotion(elements[0]), 
		0, 0, elements[1], Math.toRadians(elements[2]), Math.toRadians(elements[3]), Math.toRadians(elements[4]), Math.toRadians(elements[5]), 7836, 0.11873e-3);

		System.out.println(tle.getLine1());
		System.out.println(tle.getLine2());
		System.out.println("ecc: " + tle.getE());
		System.out.println("i: " + Math.toDegrees(tle.getI()));
		System.out.println("RAAN: " + Math.toDegrees(tle.getRaan()));
		System.out.println("PA: " + Math.toDegrees(tle.getPerigeeArgument()));
		System.out.println("MA: " + Math.toDegrees(tle.getMeanAnomaly()));
		System.out.println("MM: " + tle.getMeanMotion());
    }
    
    public int getIntervalCount(){
    	return this.intervalCount;
    }
    
    public void printAccessReport(){
    	if (intervalCount > 0){
	    	System.out.println(1 + " " + Transformations.unix2stamp(intervals.get(0)[0]) + 
	    			" -> " + Transformations.unix2stamp(intervals.get(0)[1]) + "\t" +
	    			0 + " minutes slc");
	        for (int j=1; j<intervalCount; j++){
	        	System.out.println(j+1 + " " + Transformations.unix2stamp(intervals.get(j)[0]) + 
	        			" -> " + Transformations.unix2stamp(intervals.get(j)[1]) + "\t" +
	        			(intervals.get(j)[0]-intervals.get(j-1)[0])/(1000*60) + " minutes slc");
	        }
    	}
    }
    
    public void printIntervalsUnix(){
    	if (intervalCount > 0){
	    	System.out.println(0 + " " + (intervals.get(0)[0]) + "\t" + (intervals.get(0)[1]) + "\t" +
	    			0 + " minutes slc");
	        for (int j=1; j<intervalCount; j++){
	        	System.out.println(j + " " + (intervals.get(j)[0]) + "\t" + (intervals.get(j)[1]) + "\t" +
	        			(intervals.get(j)[0]-intervals.get(j-1)[0])/(1000*60) + " minutes slc");
	        }
    	}
    }
    
	@Override
	public void run() {
		computeAccess();
	}
	
    
}
	


