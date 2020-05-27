package astrotools.structures;

public class AER {
	
	private double azimuth = 0.0;		
	private double elevation = 0.0;		
	private double range = 0.0;
	private double doppler = 0.0;
	private String fromId = "";
	private String toId = "";
	private LLA from = new LLA(0.0,0.0,0.0);
	private ECEF to = new ECEF(0.0,0.0,0.0);
	
	public AER (double a, double e, double r){
		this.azimuth = a;
		this.elevation = e;
		this.range = r;
	}
	
	public AER (double a, double e, double r, double doppler){
		this.azimuth = a;
		this.elevation = e;
		this.range = r;
		this.doppler = doppler;
	}

	public void setAER (double a, double e, double r){
		this.azimuth = a;
		this.elevation = e;
		this.range = r;
	}
	
	public void setNodes (LLA from, ECEF to){
		this.from = from;
		this.to = to;
	}
	
	public LLA getFrom(){
		return this.from;
	}
	
	public ECEF getTo(){
		return this.to;
	}
	
	public void setNodeIds (String from, String to){
		this.fromId = from;
		this.toId = to;
	}
	
	public String getFromId(){
		return this.fromId;
	}
	
	public String getToId(){
		return this.toId;
	}
	
	public void setDoppler (double doppler){
		this.doppler = doppler;
	}

	public double getDoppler (){
		return this.doppler;
	}
	
	public double getA(){
		return this.azimuth;
	}
	
	public double getE(){
		return this.elevation;
	}
	
	public double getR(){
		return this.range;
	}

	public void setA(double a){
		this.azimuth = a;
	}
	
	public void setE(double e){
		this.elevation = e;
	}
	
	public void setR(double r){
		this.range = r;
	}
	
}
