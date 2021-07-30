package tnefern.honeybeeframework.common;

public class AppInfo {
	private int infoMode = -1;// this gives the mode of the info stored in this
								// object. eg. stored as text, gps co-ordinates
								// etc

	private String infoString = null;
	private String id = "";
	
	public AppInfo(int pMode, String pString, String pId) {
		this.infoMode = pMode;
		this.infoString = pString;
		this.id = pId;
	}
	
	public String getStringInfo(){
		return this.infoString;
	}
	
	public void setString(String pStr){
		this.infoString = pStr;
	}
	
	public String toString(){
		return getStringInfo();
	}
	
	public int getMode(){
		return this.infoMode;
	}
	
	public String getId(){
		return this.id;
	}
	
	public void setId(String pStr){
		this.id = pStr;
	}
	
	
}
