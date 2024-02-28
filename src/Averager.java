
import java.util.Arrays;

public class Averager implements IDataConsumer {
	IDataConsumer wrappedClass;
	
	private double[] internalValue;
	private int numberOfAverages;
	private int requestedAverages;
	
	Averager(int averages, IDataConsumer target){
		wrappedClass = target;
		requestedAverages = averages;
		numberOfAverages = 0;
	}

	@Override
	public void updateData(double[] newValues) {
		if(internalValue == null || internalValue.length != newValues.length||numberOfAverages >= requestedAverages) {
			internalValue = new double[newValues.length];
			Arrays.fill(internalValue, 0);
			numberOfAverages = 0;
		} 
		for(int i=0;i<newValues.length;i++) {
			internalValue[i] += (1/(double)requestedAverages)*(newValues[i]); 	
			
		}
		numberOfAverages++;
		if(numberOfAverages >= requestedAverages) {
			wrappedClass.updateData(internalValue);
		}
		
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		internalValue = null;
		
	}

	@Override
	public void disconnectFromData() {
		internalValue = null;
		
	}
}
