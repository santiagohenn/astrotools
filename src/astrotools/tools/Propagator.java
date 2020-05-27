package astrotools.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Vector;

import astrotools.exceptions.ObjectDecayed;
import astrotools.exceptions.SatElsetException;
import astrotools.structures.*;

public class Propagator {
	
	OrbitalData sat;
	String timeStart;
	String timeEnd;
	double timeStep;	// seconds
	String type = "";
	public static Ephemerides ephemerides = new Ephemerides();
	public static long lastElapsedTime = 0;
	
	public Propagator(){
		
	}
	
	public Propagator(OrbitalData orb, String start, String end, double step, String type){
		this.sat = orb;
		this.timeStart = start;
		this.timeEnd = end;
		this.timeStep = step;
		this.type = type;
	}
	
	public Propagator(OrbitalData orb, String type){
		this.sat = orb;
		this.type = type;
	}
	
	public void setParams(OrbitalData orb, String start, String end, double step, String type){
		this.sat = orb;
		this.timeStart = start;
		this.timeEnd = end;
		this.timeStep = step;
		this.type = type;
	}
	
	public void setParams(OrbitalData orb, String type){
		this.sat = orb;
		this.type = type;
	}
	
	public void setTimes(double start, double end, double step) throws ParseException{
		this.timeStart = Transformations.unix2stamp((long) start);
		this.timeEnd = Transformations.unix2stamp((long) end);
		this.timeStep = step;
	}
	
	public Ephemerides getEphemerides(){
		return ephemerides;
	}
	
	public void propagate(){
		switch(type){
		default:
		case("SGP4"):
			runSGP4();
			break;
		}
	}
	
	public Ephemeris propagateAt(String time) throws SatElsetException, ParseException{
		switch(type){
		default:
		case("SGP4"):
			return runSGP4at(time);
		}
	}
	
	public Ephemeris propagateAt(double time) throws SatElsetException, ParseException{
		switch(type){
		default:
		case("SGP4"):
			return runSGP4at(Transformations.unix2stamp((long) time));
		}
	}
	
	public long getProcessTime(){
		return lastElapsedTime;
	}
	
	private Ephemerides runSGP4() {
		
    	ephemerides.clear();
    	ephemerides.setStep(timeStep);
    	
    	double julianStart = Transformations.stamp2julian(timeStart);
    	long unixStart = Transformations.stamp2unix(timeStart);
    	
    	// FIXME All of this is awful. I'll try to get another propagator.
    	
    	//Parsing the year, month and day
    	LocalDate dateBefore = LocalDate.parse(timeStart.substring(0, 4)+"-01-01");	// Beginning of the startTime year
    	LocalDate dateAfter = LocalDate.parse(timeStart.substring(0, 10));			// startTime Nth day of the year
    	
    	// Getting the Days between dates:
    	long noOfDaysBetween = ChronoUnit.DAYS.between(dateBefore, dateAfter)+1;	// Days passed from 01/01
    	
    	// Getting the start and stop years:
        int startYr = Integer.parseInt(timeStart.substring(2,4));	// Start year in the format the propagator likes
        int stopYr = Integer.parseInt(timeEnd.substring(2,4));		// Stop year in the format the propagator likes
    	
    	// Getting the Start Time in days:
    	LocalTime time = LocalTime.parse(timeStart.substring(11));
    	double hour = time.getHour()/24.0;
    	double minute = time.getMinute()/(24.00*60.00);
    	double second = time.getSecond()/(24.00*3600.00) + time.getNano()/(24.00*3600.00*1E9);
    	double startDay = noOfDaysBetween + hour + minute + second;		// Start time in fractional days - double format
    	
    	dateBefore = LocalDate.parse(timeEnd.substring(0, 4)+"-01-01");	// Beginning of the endTime year
    	dateAfter = LocalDate.parse(timeEnd.substring(0, 10));			// stopTime Nth day of the year
    	
    	noOfDaysBetween = ChronoUnit.DAYS.between(dateBefore, dateAfter)+1;	// Days passed from start of propagation
    	
    	// Getting the Stop Time:
    	time = LocalTime.parse(timeEnd.substring(11));
    	hour = time.getHour()/24.0;
    	minute = time.getMinute()/(24.00*60.00);
    	second = time.getSecond()/(24.00*3600.00) + time.getNano()/(24.00*3600.00*1E9);
    	
        double stopDay = noOfDaysBetween + hour + minute + second;
        
        OrbitalData data = null;
        
        lastElapsedTime = System.currentTimeMillis();
        
        try {

            SGP4 sgp4 = new SGP4();
            Vector<?> results = sgp4.runSgp4(sat.getTLE1(), sat.getTLE2(), startYr, startDay,
                    stopYr, stopDay, timeStep/60); // The propagator uses minutes

            for (int i = 0; i < results.size(); i++) {
            	
                data = (OrbitalData) results.elementAt(i);
                
                TEME teme = new TEME(data.getX(),data.getY(),data.getZ());
                teme.scale(1000*6378.135);
                
                Ephemeris eph = new Ephemeris(unixStart,teme);
                eph.setJulianDate(julianStart);
                eph.setUnixDate(unixStart);
                
                Date unixDate = new Date(unixStart);
                String dateStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(unixDate);
                
                ephemerides.setEphemerisAt(dateStamp, eph);
                unixStart = unixStart + (long)(timeStep*1000);		// Epoch uses milliseconds
                julianStart = julianStart + (timeStep/(24.0*60*60));	// Julian is in fractional days
            }
            
            lastElapsedTime = System.currentTimeMillis() - lastElapsedTime;
            
        } catch (ObjectDecayed ioe) {
            System.out.println("decayed " + ioe);
        } catch (SatElsetException ioe) {
            System.out.println("elset exception " + ioe);
        }
        
		return new Ephemerides();
	}

	private Ephemeris runSGP4at(String timeAt) throws SatElsetException, ParseException{
    	
    	double julianStart = Transformations.stamp2julian(timeAt);
    	long unixStart = Transformations.stamp2unix(timeAt);
    	
    	// FIXME All of this is awful. I'll try to get another propagator.
    	
    	//Parsing the year, month and day
    	LocalDate dateBefore = LocalDate.parse(timeAt.substring(0, 4)+"-01-01");	// Beginning of the startTime year
    	LocalDate dateAfter = LocalDate.parse(timeAt.substring(0, 10));			// startTime Nth day of the year
    	
    	// Getting the Days between dates:
    	long noOfDaysBetween = ChronoUnit.DAYS.between(dateBefore, dateAfter)+1;	// Days passed from 01/01
    	
    	// Getting the start and stop years:
        int startYr = Integer.parseInt(timeAt.substring(2,4));	// Start year in the format the propagator likes
    	
    	// Getting the Start Time in days:
    	LocalTime time = LocalTime.parse(timeAt.substring(11));
    	double hour = time.getHour()/24.0;
    	double minute = time.getMinute()/(24.00*60.00);
    	double second = time.getSecond()/(24.00*3600.00) + time.getNano()/(24.00*3600.00*1E9);
    	double startDay = noOfDaysBetween + hour + minute + second;		// Start time in fractional days - double format

        Ephemeris eph = new Ephemeris();
        
        try {

            SGP4 sgp4 = new SGP4();
            OrbitalData data = sgp4.runSgp4(sat.getTLE1(), sat.getTLE2(), startYr,startDay);

            TEME teme = new TEME(data.getX(),data.getY(),data.getZ());
            teme.scale(1000*6378.135); // to meters * radius earth
            
            eph = new Ephemeris(unixStart,teme);
            eph.setJulianDate(julianStart);
            eph.setUnixDate(unixStart);
            
        } catch (ObjectDecayed ioe) {
            System.out.println("decayed " + ioe);
        }
        
        return eph;
	}
	
}
