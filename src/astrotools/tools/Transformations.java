package astrotools.tools;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import javax.vecmath.Vector3d;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import astrotools.exceptions.SatElsetException;
import astrotools.structures.*;

public class Transformations {
	
    public static final double earthRadius = 6378135;	//Radius Earth [m]; WGS-84 (semi-major axis, a) (Equatorial Radius)
    public static final double eccentricity = 8.1819190842622e-2;	//Ellipsoid constants: eccentricity; WGS84
    public static final double mu =  3.986004418e+14; // gravitation coefficient
    
    public static int JGREG = 15 + 31 * (10 + 12 * 1582);

    private static final double asq = Math.pow(earthRadius, 2);
    private static final double esq = Math.pow(eccentricity, 2);

    /**
     * longitude in radians.
     * latitude in radians.
     * altitude in meters.
     *
     * @param ecef
     */
    
    public static LLA ecef2lla(ECEF ecef) {
        double x = ecef.getX();
        double y = ecef.getY();
        double z = ecef.getZ();

        double b = Math.sqrt(asq * (1 - esq));
        double bsq = Math.pow(b, 2);
        double ep = Math.sqrt((asq - bsq) / bsq);
        double p = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double th = Math.atan2(earthRadius * z, b * p);

        double lon = Math.atan2(y, x);
        double lat = Math.atan2((z + Math.pow(ep, 2) * b * Math.pow(Math.sin(th), 3)), (p - esq * earthRadius * Math.pow(Math.cos(th), 3)));
        double N = earthRadius / (Math.sqrt(1 - esq * Math.pow(Math.sin(lat), 2)));
        double alt = p / Math.cos(lat) - N;

        // mod lat to 0-2pi
        lon = lon % (2 * Math.PI);
        // lat = lat % (2 * Math.PI);
        
        double rArray[] = new double[]{x, y, z};

        if (norm(rArray) <= 0) {
            lon = 0.0;
            lat = 0.0;
            alt = -earthRadius;
        }

        return new LLA(lat, lon, alt);
    }
    
    public static LLA ecef2lla(Vector3d ecef) {
        double x = ecef.getX();
        double y = ecef.getY();
        double z = ecef.getZ();

        double b = Math.sqrt(asq * (1 - esq));
        double bsq = Math.pow(b, 2);
        double ep = Math.sqrt((asq - bsq) / bsq);
        double p = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double th = Math.atan2(earthRadius * z, b * p);

        double lon = Math.atan2(y, x);
        double lat = Math.atan2((z + Math.pow(ep, 2) * b * Math.pow(Math.sin(th), 3)), (p - esq * earthRadius * Math.pow(Math.cos(th), 3)));
        double N = earthRadius / (Math.sqrt(1 - esq * Math.pow(Math.sin(lat), 2)));
        double alt = p / Math.cos(lat) - N;

        // mod lat to 0-2pi
        lon = lon % (2 * Math.PI);
        // lat = lat % (2 * Math.PI);
        
        double rArray[] = new double[]{x, y, z};

        if (norm(rArray) <= 0) {
            lon = 0.0;
            lat = 0.0;
            alt = -earthRadius;
        }

        return new LLA(lat, lon, alt);
    }

    public static ECEF lla2ecef(LLA lla) {
        double lat = lla.getLatitude();
        double lon = lla.getLongitude();
        double alt = lla.getAltitude();

        double N = earthRadius / Math.sqrt(1 - esq * Math.pow(Math.sin(lat), 2));

        double x = (N + alt) * Math.cos(lat) * Math.cos(lon);
        double y = (N + alt) * Math.cos(lat) * Math.sin(lon);
        double z = ((1 - esq) * N + alt) * Math.sin(lat);

        return new ECEF(x, y, z);
    }

    public static ECEF lla2ecef(Vector3d lla) {
        double lat = lla.getX();
        double lon = lla.getY();
        double alt = lla.getZ();

        double N = earthRadius / Math.sqrt(1 - esq * Math.pow(Math.sin(lat), 2));

        double x = (N + alt) * Math.cos(lat) * Math.cos(lon);
        double y = (N + alt) * Math.cos(lat) * Math.sin(lon);
        double z = ((1 - esq) * N + alt) * Math.sin(lat);

        return new ECEF(x, y, z);
    }

    public static ECEF teme2ecef(Vector3d teme, double julianDate) {
    	
        double gmst = 0;
        double st[][] = new double[3][3];
        double rpef[] = new double[3];
        double pm[][] = new double[3][3];

        //Get Greenwich mean sidereal time
        gmst = greenwichMeanSidereal(julianDate);

        //st is the pef - tod matrix
        st[0][0] = cos(gmst);
        st[0][1] = -sin(gmst);
        st[0][2] = 0.0;
        st[1][0] = sin(gmst);
        st[1][1] = cos(gmst);
        st[1][2] = 0.0;
        st[2][0] = 0.0;
        st[2][1] = 0.0;
        st[2][2] = 1.0;

        //Get pseudo earth fixed position vector by multiplying the inverse pef-tod matrix by rteme
        rpef[0] = st[0][0] * teme.getX() + st[1][0] * teme.getY() + st[2][0] * teme.getZ();
        rpef[1] = st[0][1] * teme.getX() + st[1][1] * teme.getY() + st[2][1] * teme.getZ();
        rpef[2] = st[0][2] * teme.getX() + st[1][2] * teme.getY() + st[2][2] * teme.getZ();

        //Get polar motion vector
        polarm(julianDate, pm);

        //ECEF postion vector is the inverse of the polar motion vector multiplied by rpef
        double x = pm[0][0] * rpef[0] + pm[1][0] * rpef[1] + pm[2][0] * rpef[2];
        double y = pm[0][1] * rpef[0] + pm[1][1] * rpef[1] + pm[2][1] * rpef[2];
        double z = pm[0][2] * rpef[0] + pm[1][2] * rpef[1] + pm[2][2] * rpef[2];

        return new ECEF(x, y, z);
    }
    

    public static ECEF teme2ecef(TEME teme, double julianDate) {
    	
        double gmst = 0;
        double st[][] = new double[3][3];
        double rpef[] = new double[3];
        double pm[][] = new double[3][3];

        //Get Greenwich mean sidereal time
        gmst = greenwichMeanSidereal(julianDate);

        //st is the pef - tod matrix
        st[0][0] = cos(gmst);
        st[0][1] = -sin(gmst);
        st[0][2] = 0.0;
        st[1][0] = sin(gmst);
        st[1][1] = cos(gmst);
        st[1][2] = 0.0;
        st[2][0] = 0.0;
        st[2][1] = 0.0;
        st[2][2] = 1.0;

        //Get pseudo earth fixed position vector by multiplying the inverse pef-tod matrix by rteme
        rpef[0] = st[0][0] * teme.getX() + st[1][0] * teme.getY() + st[2][0] * teme.getZ();
        rpef[1] = st[0][1] * teme.getX() + st[1][1] * teme.getY() + st[2][1] * teme.getZ();
        rpef[2] = st[0][2] * teme.getX() + st[1][2] * teme.getY() + st[2][2] * teme.getZ();

        //Get polar motion vector
        polarm(julianDate, pm);

        //ECEF postion vector is the inverse of the polar motion vector multiplied by rpef
        double x = pm[0][0] * rpef[0] + pm[1][0] * rpef[1] + pm[2][0] * rpef[2];
        double y = pm[0][1] * rpef[0] + pm[1][1] * rpef[1] + pm[2][1] * rpef[2];
        double z = pm[0][2] * rpef[0] + pm[1][2] * rpef[1] + pm[2][2] * rpef[2];

        return new ECEF(x, y, z);
    }

    public static Ephemerides getAER(LLA from, Ephemerides target){

        //  Iterate through Ephemerides:
        for (String key : target.getMap().keySet()){
        	
        	// Take Ephemeris
	        Ephemeris eph = target.getEphemerisAt(key);
	        
	        AER aer = getAER(from,eph);
        	eph.setAER(aer);
        	target.setEphemerisAt(key, eph);
        }
		return target;
    }
    
    public static AER getAER(LLA from, Ephemeris target){
    	
        // configure Orekit FIXME check if necessary
        File orekitData = new File("orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
    	
    	// Point on earth and frame:
        double latitude = FastMath.toRadians(from.getLatitude());
        double longitude  = FastMath.toRadians(from.getLongitude());
        double altitude  = from.getAltitude();
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     earthFrame);
        GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
        TopocentricFrame staF = new TopocentricFrame(earth, station, "station");

        //  Initial state definition : date, orbit
        TimeScale utc = TimeScalesFactory.getUTC();
        Frame inertialFrame = FramesFactory.getTEME();
    
        // Time of Ephemeris:
    	long unix = target.getUnixDate();
        Date unixDate = new Date(unix);
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(unixDate);
    	
    	//Parsing the date
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        
    	//Parsing the time
    	LocalTime now = LocalTime.parse(date.substring(11));
    	int hh = now.getHour();
    	int mm = now.getMinute();
    	double ss = now.getSecond() + now.getNano()/1E9;
        
        AbsoluteDate initialDate = new AbsoluteDate(year, month, day, hh, mm, ss, utc);
        
        TEME pos = target.getTEME();
        Vector3D posisat = new Vector3D(pos.getX(),pos.getY(),pos.getZ());
        
        double EL = staF.getElevation(posisat, inertialFrame, initialDate);
        double AZ = staF.getAzimuth(posisat, inertialFrame, initialDate);
        double range = staF.getRange(posisat, inertialFrame, initialDate);
        
        AER aer = new AER(AZ,EL,range);
        
        return aer;
    }

    public Ephemerides getAERD(LLA from, Ephemerides target){
    	
        for (String key : target.getMap().keySet()){
        	
        	// Take Ephemeris
	        Ephemeris eph = target.getEphemerisAt(key);
	        
	        AER aer = getAERD(from,eph);
        	eph.setAER(aer);
        	target.setEphemerisAt(key, eph);
        }
        
        return target;
    }
    
    public AER getAERD(LLA from, Ephemeris target){
    	
    	// Point on earth and frame:
        double longitude = FastMath.toRadians(from.getLatitude());
        double latitude  = FastMath.toRadians(from.getLongitude());
        double altitude  = from.getAltitude();
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     earthFrame);
        GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
        TopocentricFrame staF = new TopocentricFrame(earth, station, "station");

        // configure Orekit FIXME check if necessary
        File orekitData = new File("orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        
        //  Initial state definition : date, orbit
        TimeScale utc = TimeScalesFactory.getUTC();
        Frame inertialFrame = FramesFactory.getTEME();
    
        // Time of Ephemeris:
    	long unix = target.getUnixDate();
        Date unixDate = new Date(unix);
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(unixDate);
    	
    	//Parsing the date
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        
    	//Parsing the time
    	LocalTime now = LocalTime.parse(date.substring(11));
    	int hh = now.getHour();
    	int mm = now.getMinute();
    	double ss = now.getSecond() + now.getNano()/1E9;
        
        AbsoluteDate initialDate = new AbsoluteDate(year, month, day, hh, mm, ss, utc);

        TEME pos = target.getTEME();
        
        Vector3D posisat = new Vector3D(pos.getX(),pos.getY(),pos.getZ());
        Vector3D velosat = new Vector3D(pos.getXdot(),pos.getYdot(),pos.getZdot());
        
        final PVCoordinates pvInert   = new PVCoordinates(posisat,velosat);
        final PVCoordinates pvStation = inertialFrame.getTransformTo(staF, initialDate).transformPVCoordinates(pvInert);
        
        // And then calculate the Doppler signal
        final double doppler = Vector3D.dotProduct(pvStation.getPosition(), pvStation.getVelocity()) / pvStation.getPosition().getNorm();

        // FIXME Check this
        
        double EL = staF.getElevation(posisat, inertialFrame, initialDate);
        double AZ = staF.getAzimuth(posisat, inertialFrame, initialDate);
        double range = staF.getRange(posisat, inertialFrame, initialDate);
        
        AER aer = new AER(AZ,EL,range,doppler);
        
        return aer;
    }
    
    public static double stamp2julian(String dateStamp) {
    	// YYYY-MM-DD
    	// HH:MM:SS
    	String date = dateStamp.substring(0,10);
    	String time = dateStamp.substring(11);
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        double hour = (Integer.parseInt(time.substring(0, 2)) - 12) / 24.0;
        double minute = Integer.parseInt(time.substring(3, 5)) / 1440.0;
        double second = Double.parseDouble(time.substring(6)) / 86400.0;		// minor fix

        int julianYear = year;
        if (year < 0) julianYear++;
        int julianMonth = month;
        if (month > 2) {
            julianMonth++;
        } else {
            julianYear--;
            julianMonth += 13;
        }
        double julian = (java.lang.Math.floor(365.25 * julianYear)
                + java.lang.Math.floor(30.6001 * julianMonth) + day + 1720995.0);
        if (day + 31 * (month + 12 * year) >= JGREG) {
            // change over to Gregorian calendar
            int ja = (int) (0.01 * julianYear);
            julian += 2 - ja + (0.25 * ja);
        }
        
        return julian + hour + minute + second;
    }

    public static long stamp2unix(String dateStamp){
    	
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS"); //FIXME FOR SPEED
        //dateFormat.setTimeZone(TimeZone.getTimeZone("UTCG"));
		Date parsedDate = new Date();
		try {
			parsedDate = dateFormat.parse(dateStamp);
			return parsedDate.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
        return parsedDate.getTime();
    }
    
    public static String unix2stamp(double unix){
        return unix2stamp((long) unix);
    }
    
    public static String unix2stamp(long unix){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTCG"));
		Date date = new Date(unix);
        return dateFormat.format(date);
    }
    
    static void polarm(double jdut1, double pm[][]) {
        double MJD; //Julian Date - 2,400,000.5 days
        double A;
        double C;
        double xp; //Polar motion coefficient in radians
        double yp; //Polar motion coefficient in radians

        //Predict polar motion coefficients using IERS Bulletin - A (Vol. XXVIII No. 030)
        MJD = jdut1 - 2400000.5;

        A = 2 * Math.PI * (MJD - 57226) / 365.25;
        C = 2 * Math.PI * (MJD - 57226) / 435;

        xp = (0.1033 + 0.0494 * cos(A) + 0.0482 * sin(A) + 0.0297 * cos(C) + 0.0307 * sin(C)) * 4.84813681e-6;
        yp = (0.3498 + 0.0441 * cos(A) - 0.0393 * sin(A) + 0.0307 * cos(C) - 0.0297 * sin(C)) * 4.84813681e-6;

        pm[0][0] = cos(xp);
        pm[0][1] = 0.0;
        pm[0][2] = -sin(xp);
        pm[1][0] = sin(xp) * sin(yp);
        pm[1][1] = cos(yp);
        pm[1][2] = cos(xp) * sin(yp);
        pm[2][0] = sin(xp) * cos(yp);
        pm[2][1] = -sin(yp);
        pm[2][2] = cos(xp) * cos(yp);
    }

    /**
     * Calculates the Greenwich mean sidereal time (GMST) on julDate (doesn't have to be 0h).
     * Used calculations from Meesus 2nd ed.
     * https://stackoverflow.com/questions/32263754/modulus-in-pascal
     *
     * @param jdut1 Julian Date
     * @return Greenwich mean sidereal time in degrees (0-360)
     */
    public static double greenwichMeanSidereal(double jdut1) {
        double Tu = (jdut1 - 2451545.0);
        double gmst = Tu * 24.06570982441908 + 18.697374558;
        gmst = (gmst % 24) * Math.PI / 12;
        return gmst;
    }
    
    public static double norm(double[] a) {
        double c = 0.0;

        for (int i = 0; i < a.length; i++) {
            c += a[i] * a[i];
        }

        return Math.sqrt(c);
    }
    
    public static double getPeriodMinutes(String tle1, String tle2) throws SatElsetException{
    	SatElset dataFromTLE = new SatElset(tle1, tle2);
        return (24*60)/(dataFromTLE.getMeanMotion());
    }
    
    public static double getPeriodMinutes(OrbitalData sat){
    	SatElset dataFromTLE = null;
		try {
			dataFromTLE = new SatElset(sat.getTLE1(), sat.getTLE2());
			return (24*60)/(dataFromTLE.getMeanMotion());
		} catch (SatElsetException e) {
			e.printStackTrace();
		}
        return 0;
    }
    
	public static double getMaxAccess(OrbitalData sat, double th){
		
		double RE = 6378135;
		
        String card1 = sat.getTLE1();
        String card2 = sat.getTLE2();
        
        double[] params = new double[6];
		try {
			params = Transformations.tle2elements(card1,card2);
		} catch (SatElsetException e) {
			e.printStackTrace();
		}
        double Rp = params[0]*(1+params[1]);	// Get the radius of the apogee
        double Hmax = Rp-RE;
        
        double etaMax = Math.asin((RE*Math.cos(th))/(RE+Hmax));
        double Tseg = (params[6]/180)*(90-th-Math.toDegrees(etaMax));
        
        return Tseg;
		
	}
	
	public static double getMaxAccess(double[] params, double th){
		
        double Ra = params[0]*(1+params[1]);	// Get the radius of the apogee
        double Hmax = Ra-earthRadius;
        
        double etaMax = Math.asin((earthRadius*Math.cos(Math.toRadians(th)))/(earthRadius+Hmax));
        double Tseg = (Transformations.getPeriodSeconds(params[0])/180)*(90-th-Math.toDegrees(etaMax));
        
        return Tseg;
	}
	
    public int getAccessRegions(LLA gs, OrbitalData sat, double th){

		final double RE = 6378135;
		double lat = Math.toRadians(gs.getLatitude());	// 50
		double inc = Math.toRadians(62.5);	// 62.5
		
		int regions = 0;
		
		if (lat<0) lat = Math.abs(lat);
		
		double Hmax = 622000;
		
        double etaMax = Math.asin((RE*Math.cos(Math.toRadians(th)))/(RE+Hmax));
		double lambdaMax = 90-th-Math.toDegrees(etaMax);
		
		lambdaMax = Math.toRadians(lambdaMax);

		if (lat >= (lambdaMax+inc)){
			regions = 0;
		} else if ((inc + lambdaMax > lat) && (lat >= inc - lambdaMax)){
			regions = 1;
		} else {
			regions = 2;
		}
		
		return regions;
	}
    
    public static double[] tle2elements(String tle1, String tle2) throws SatElsetException{
    	
    	double[] elements = new double[7];
    	SatElset data = new SatElset(tle1, tle2);
    	double period = (24*60*60)/data.getMeanMotion();
    	elements[0] = Math.pow((mu*Math.pow(period/(2*Math.PI), 2)), 1.0/3.0);
    	System.out.println("A: " + elements[0]);
    	elements[1] = data.getEccentricity();
    	System.out.println("e: " + elements[1]);
    	System.out.println("mean motion: " + getMeanMotion(elements[0]));
    	
    	elements[2] = data.getInclinationDeg();
    	elements[3] = data.getRightAscensionDeg();
    	elements[4] = data.getArgPerigeeDeg();
    	elements[5] = data.getMeanAnomalyDeg();		// Gets the mean anomaly!.
    	elements[6] = period;	// Why not just include it
        return elements;
    }
    
    public static double getPeriodSeconds(double a){
    	return 2*Math.PI*Math.sqrt(Math.pow(a, 3.0)/mu);
    }
    
    public static double getMeanMotion(double a){
    	return Math.sqrt(mu/(Math.pow(a, 3.0)));
    }
    
}
