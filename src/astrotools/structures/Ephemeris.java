package astrotools.structures;

public class Ephemeris {

	private long unixDate;
	private double julianDate;
	private TEME teme;
	private ECEF ecef;
	private LLA lla;
	private AER aer;
	
	public Ephemeris(){
		
	}
	
	public Ephemeris(long unix, TEME teme){
		this.unixDate = unix;
		this.teme = teme;
	}
	
	public Ephemeris(double julian, TEME teme){
		this.julianDate = julian;
		this.teme = teme;
	}
	
	public void setJulianDate(double julian){
		this.julianDate = julian;
	}
	
	public double getJulianDate(){
		return julianDate;
	}
	
	public void setUnixDate(long unix){
		this.unixDate = unix;
	}
	
	public long getUnixDate(){
		return unixDate;
	}
	
	public void setTEME(TEME teme){
		this.teme = teme;
	}
	
	public TEME getTEME(){
		return this.teme;
	}
	
	public void setECEF(ECEF ecef){
		this.ecef = ecef;
	}
	
	public ECEF getECEF(){
		return this.ecef;
	}
	
	public void setLLA(LLA lla){
		this.lla = lla;
	}
	
	public LLA getLLA(){
		return this.lla;
	}
	
	public void setAER(AER aer){
		this.aer = aer;
	}
	
	public AER getAER(){
		return this.aer;
	}
	
}
