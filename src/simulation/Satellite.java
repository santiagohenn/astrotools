package simulation;

import astrotools.structures.OrbitalData;

public class Satellite {

	String id = "";
	public String tle1 = "";
	public String tle2 = "";
	public double[] elements = new double[6];
	OrbitalData orbit = new OrbitalData();
	
	public Satellite(){
		this.id = "";
	}
	
	public Satellite(String id, double[] elements){
		this.id = id;
		this.elements = elements;
		orbit.setElements(elements);
	}
	
	public Satellite(String id, String card1, String card2){
		this.id = id;
		this.tle1 = card1;
		this.tle2 = card2;
		orbit.setTLE(card1, card2);
	}
	
	public OrbitalData getOrbit(){
		return this.orbit;
	}
	
	public void setElements(double[] params){
		this.elements = params;
	}
	
	public void setElement(int element, double value){
		this.elements[element] = value;
	}
	
	public double[] getElements(){
		return this.elements;
	}
	
	public String getTLE1(){
		return this.tle1;
	}
	
	public String getTLE2(){
		return this.tle2;
	}
	
	public String getElementsReport(){
		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf(elements[0]));
		for (int i = 1; i<6; i++){
			sb.append(",");
			sb.append(String.valueOf(elements[i]));
		}
		return sb.toString();
	}
	
	public String getId(){
		return this.id;
	}
	
}
