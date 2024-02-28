import java.util.ArrayDeque;

import org.apache.commons.numbers.complex.Complex;

public class PositivePassFilter implements ISignalChainBlock  {
	private IDataConsumer downstreamModule;
	ISignalChainBlock hilbertTransformer;
	
	private class CallBack implements IDataConsumer{

		@Override
		public void updateData(Complex[] newValues) {
			PositivePassFilter.this.updateTransformed(newValues);
			PositivePassFilter.this.processData();			
		}

		@Override
		public void connectToData(DataStreamInformation streamInformation) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void disconnectFromData() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateData(double[] newValues) {
			// TODO Auto-generated method stub
			
		}


		
	}

	private PositivePassFilter.CallBack hillbertCallback;
	private ArrayDeque<Complex> buffer;
	private Complex[] transformed;
	private int delay;
	private int blockSize = 0;
	private boolean transformedFlag;
	
	PositivePassFilter(ISignalChainBlock hilbertTransformer, int delay){
		this.hilbertTransformer = hilbertTransformer;
		this.hillbertCallback = new CallBack();
		this.hilbertTransformer.setDownstreamModule(hillbertCallback);
		this.delay = delay;
		this.buffer = new ArrayDeque<Complex>();
	}
	
	private void updateTransformed(Complex[] newValues) {
	       this.transformed = newValues;
	       this.transformedFlag = true;
	}
	
	@Override
	public void updateData(Complex[] newValues) {
		if(this.blockSize != newValues.length) {
			this.buffer.clear();
			this.blockSize = newValues.length;
			for(int i=0;i<this.delay;i++) { 
				this.buffer.add(Complex.ofCartesian(0, 0));
			}
		} 
		for(int i=0;i<newValues.length;i++) { 
			this.buffer.add(newValues[i]);
		}
		this.transformedFlag = false;
		this.hilbertTransformer.updateData(newValues);
   	}
	
	private void processData() {

		Complex [] output = new Complex[this.blockSize];
		if(this.transformedFlag == true) {
			int i;
			for(i = 0;i<transformed.length;i++) {
				output[i] = this.buffer.removeFirst().add(this.transformed[i].multiply(Complex.I));
			
			}
			this.downstreamModule.updateData(output);
		}
		this.transformedFlag = false;
   	}
	

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		this.buffer.clear();
		this.blockSize = 0;
		if(downstreamModule != null) {
			downstreamModule.connectToData( streamInformation);
		}	
		this.hilbertTransformer.connectToData( streamInformation);
	}

	@Override
	public void disconnectFromData() {
		this.downstreamModule.disconnectFromData();
	}

	@Override
	public void setDownstreamModule(IDataConsumer target) {
		downstreamModule = target;
		
	}


}
