
public class Decimation implements ISignalChainBlock {
	

	/*
	 * inputDataRate = samples per second of incoming data
	 * outputDataRate = samples per second of the output data
	 * This method will perform decimation with linear interpolation
	 *
	 */
	private double fractionalRate;
	private double savedPointFromLastUpdate = 0;
	private long sampleCounter;
	private IDataConsumer downstreamModule;
	private double[] outputBuffer;
	private int outputSize; //Amount waiting in outputBuffer;
	
	Decimation(double inputDataRate, double audioSampleRate){
		fractionalRate = audioSampleRate/inputDataRate;
	}

	@Override
	public void updateData(double[] newValues) {
		
		//Ensure that the number of samples in each update is preserved from output to input
		if(outputBuffer ==null || outputBuffer.length != newValues.length) {
			outputBuffer = new double[newValues.length];
			this.outputSize = 0;
		}
		
		long totalDownsampledOutputs = (long) (sampleCounter*fractionalRate);
		int newOutputSamples = (int) (((sampleCounter+newValues.length)*fractionalRate) - totalDownsampledOutputs) ;
		double[] output = new double[newOutputSamples];
		int j = 0;
		for(int i=0;i<newValues.length;i++) {
			sampleCounter++;
			if((long)(sampleCounter*fractionalRate) > totalDownsampledOutputs) {
				//Current sample is latter in time than the next decimated output
				//double relativeSampleTime = (double)(totalDownsampledOutputs)/fractionalRate - (sampleCounter-1);
				if(i==0) {
					//output[j] = this.savedPointFromLastUpdate + relativeSampleTime*(newValues[0]-this.savedPointFromLastUpdate);
					output[j] = this.savedPointFromLastUpdate;
				} else {
					//output[j] = newValues[i-1] + relativeSampleTime*(newValues[i]-newValues[i-1]);
					output[j] = newValues[i]; 
				}
				
				j++;
				totalDownsampledOutputs++;
			}
		}
		
		if(this.outputSize + newOutputSamples >= this.outputBuffer.length) {
			//It is possible to output a full size block
			
			int amountCopiedToOutput =  this.outputBuffer.length - this.outputSize;
			System.arraycopy(output, 0, this.outputBuffer, this.outputSize,amountCopiedToOutput);
			downstreamModule.updateData(this.outputBuffer);
			this.outputSize = 0;
			System.arraycopy(output, amountCopiedToOutput, this.outputBuffer, 0, newOutputSamples - amountCopiedToOutput );
			this.outputSize += newOutputSamples - amountCopiedToOutput;
		} else {
			System.arraycopy(output, 0, this.outputBuffer, this.outputSize, newOutputSamples);
			this.outputSize += newOutputSamples;
		}
		this.savedPointFromLastUpdate = newValues[newValues.length-1];
	}
	

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		this.sampleCounter =0;
		this.outputBuffer = null;
		if(downstreamModule != null) {
				DataStreamInformation newInfo = streamInformation.clone();
				newInfo.addAttribute(DataStreamInformation.SAMPLERATE, fractionalRate* (Double)streamInformation.getAttribute(DataStreamInformation.SAMPLERATE));
				downstreamModule.connectToData(streamInformation);
			}	
	}

	@Override
	public void disconnectFromData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDownstreamModule(IDataConsumer target) {
		downstreamModule = target;
		
	}

}
