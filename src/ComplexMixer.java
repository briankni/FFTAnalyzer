

import org.apache.commons.numbers.complex.Complex;

public class ComplexMixer implements ISignalChainBlock{
	double frequency;
	long sample = 0;
	IDataConsumer downstreamModule;
	//F is 
	public ComplexMixer(double f){
		frequency = f;
	}
	private synchronized Complex sample() {
		Complex result = Complex.ofCartesian(Math.cos(sample*2*Math.PI*frequency),-1*Math.sin(sample*2*Math.PI*frequency));
		sample++;
		return result;
	}
	
	public synchronized void setFrequency(double f) {
		this.frequency = f;
	}
	@Override
	public void updateData(double[] newValues) {
		Complex[] ouputComplex = new Complex[newValues.length];
		for (int i = 0; i < newValues.length; i++) {
			ouputComplex[i] = this.sample().multiply(newValues[i]);

        }
		if(this.downstreamModule != null) {
			downstreamModule.updateData(ouputComplex);
		}
		
	}
	@Override
	public void updateData(Complex[] newValues) {
		Complex[] ouputComplex = new Complex[newValues.length];
		for (int i = 0; i < newValues.length; i++) {
			ouputComplex[i] = this.sample().multiply(newValues[i]);

        }
		if(this.downstreamModule != null) {
			downstreamModule.updateData(ouputComplex);
		}
		
	}
	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		if(downstreamModule != null) {
			downstreamModule.connectToData(streamInformation);
		}	
	}
	
	@Override
	public void disconnectFromData() {
		//Do nothing
		
	}
	@Override
	public void setDownstreamModule(IDataConsumer target) {
		downstreamModule = target;
		
	}
}