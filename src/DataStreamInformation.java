import java.util.HashMap;

public class DataStreamInformation {
	private HashMap<String,Object> dataTable;
	public static String SAMPLERATE = "Sample Rate";
	public static String MINVALUE = "Minimum Value";
	public static String MAXVALUE = "Maximum Value";

	//All data must have at least at rate and range
	DataStreamInformation(double sampleRate, double min, double max){
		dataTable = new HashMap<String, Object>();
		this.addAttribute(SAMPLERATE, sampleRate);
		this.addAttribute(MINVALUE, min);
		this.addAttribute(MAXVALUE,max);
	}
	
	public void addAttribute(String key, Object value) {
		dataTable.put(key, value);
	}
	
	public Object getAttribute(String key) {
		return dataTable.get(key);
	}
	
	public DataStreamInformation clone() {
		DataStreamInformation newDS = new DataStreamInformation((Double) this.getAttribute(SAMPLERATE),(Double) this.getAttribute(MINVALUE),(Double) this.getAttribute(MAXVALUE));
		dataTable.forEach((k,v) ->
			newDS.addAttribute(k, v)
				);
		return newDS;
	}
}
