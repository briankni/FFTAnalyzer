




import java.util.Arrays;

import org.apache.commons.math4.transform.FastFourierTransform;
import org.apache.commons.numbers.complex.Complex;

class FIRFilter implements ISignalChainBlock{
	
	static int BLOCKSIZE = 1024; //Size of convolution to use
	
	Complex[] waitingInputData = new Complex[BLOCKSIZE];  //If the input does not neatly fit into units of BLOCKSIZE, remainingData stores remaining elements until the next update
	Complex[] overlapData;
	int waitingInputDataLength = 0;
	private IDataConsumer downstreamModule;
	private Complex[] transformedKernel;
	private FastFourierTransform transform;
	private FastFourierTransform inverse;
	private int kernelLength = 0;
	private int outputSize=0;
	private Complex[] outputBuffer = null;
	
	FIRFilter(double[] filterKernel){
		this.kernelLength = filterKernel.length;
		double[] paddedFilterKernel = Arrays.copyOf(filterKernel, BLOCKSIZE);
		overlapData = new Complex[this.kernelLength-1];
		
		downstreamModule = null;
		 
		transform = new FastFourierTransform(FastFourierTransform.Norm.STD,false);
		inverse = new FastFourierTransform(FastFourierTransform.Norm.STD,true);
		
		for(int i=0;i<BLOCKSIZE;i++) {
			waitingInputData[i] = Complex.ofCartesian(0, 0);
		}
		for(int i=0;i<this.kernelLength-1;i++) {
			overlapData[i] = Complex.ofCartesian(0, 0);
		}
		
		
		transformedKernel = transform.apply(paddedFilterKernel);
		
	}
	
	private static double hannWindow(int i, int sequenceLength) {
			return 2*0.5*(1-Math.cos((2*Math.PI*i)/sequenceLength));
	}
	
	private static double hilbertTransformImpulseResponse(int n) {
		if(n==0) {
			return 0.0;
		} else {
			return (2.0/(Math.PI*n))*Math.pow(Math.sin(Math.PI*n*0.5),2);
		}
	}
	
	static double[] hilbertTransformerKernel(int MOver2) {
		int M = MOver2*2; //Has to be even.   Length is M+1
		double[] filterKernel = new double[M+1];
		Complex normalizationSum = Complex.ofCartesian(0, 0);
		for(int n=0;n<=M;n++) {
			filterKernel[n] = 1.4*hannWindow(n,M+1)*hilbertTransformImpulseResponse(n-MOver2);
			//Normalize at 1/10th the sampling rate
			double f = .1;
			normalizationSum = normalizationSum.add(Complex.ofCartesian(Math.cos(-1*n*2*Math.PI*f),Math.sin(-1*n*2*Math.PI*f)).multiply(filterKernel[n]));
		}
		double mag = Math.sqrt(normalizationSum.norm());
		for(int i=0;i<=M;i++) {
		
			filterKernel[i] /= mag;
		}
		return filterKernel;
	}

	static double[] lowpassFilterKernel(double fc, int MOver2) {
		
		int M = MOver2*2; //Has to be even
		double[] filterKernel = new double[M+1];
		double normalizationSum = 0;
		for(int i=0;i<=M;i++) {
			if(i==(M/2)) {
				filterKernel[i] = 2*Math.PI*fc;
			} else {
				filterKernel[i] =( Math.sin(2*Math.PI*fc*(i-M/2))/(i-M/2)) * (0.42 - 0.5*Math.cos(2*Math.PI*i/M) + 0.08*Math.cos(4*Math.PI*i/M) ) ;
			}
			normalizationSum += filterKernel[i];
		}
		for(int i=0;i<=M;i++) {
			filterKernel[i] /= normalizationSum;
		}
		return filterKernel;
	}
	
	private void dotProductInPlace(Complex[] target, Complex[] kernel) {
		for(int i=0;i<(target.length);i++) {
			target[i] = target[i].multiply(kernel[i]);
		}
	}
	
	@Override
	public void updateData(Complex[] newValues) {
		
		int totalDataLength = this.waitingInputDataLength + newValues.length;
		int dataPerBlock = 1+BLOCKSIZE - this.kernelLength;  //Number of data samples processed per FFT
															 //Ex: kernel of 400 and FFT of 1024 +1 = 625 samples 
		int numberOfFFTsToPerform = totalDataLength/(dataPerBlock);
		
		//Ensure that the number of samples in each update is preserved from output to input
		if(outputBuffer ==null || outputBuffer.length != newValues.length) {
			outputBuffer = new Complex[newValues.length];
			this.outputSize = 0;
		}
		
		
		if (numberOfFFTsToPerform==0) {
			System.arraycopy(newValues, 0, this.waitingInputData, waitingInputDataLength, newValues.length);
			this.waitingInputDataLength +=newValues.length;
		}else {
			//The first block may be a combination of saved data that was too short and new data
			System.arraycopy(newValues, 0, this.waitingInputData, waitingInputDataLength, dataPerBlock-waitingInputDataLength);
			for(int i =0;i<numberOfFFTsToPerform;i++) {
				if(i>0) { //blocks after the first block are taken entirely from the new input (newValues)
					System.arraycopy(newValues, (dataPerBlock-waitingInputDataLength) + (i-1)*dataPerBlock, this.waitingInputData, 0, dataPerBlock);
				}
				Complex[] transformedValues = this.transform.apply(this.waitingInputData);
				this.dotProductInPlace(transformedValues,this.transformedKernel);
				Complex[] filtered = this.inverse.apply(transformedValues);
				//overlap and add
				for(int j =0;j<this.kernelLength-1;j++) {
					filtered[j] = filtered[j].add(overlapData[j]);
				}
				
				//Save data to overlap with the next block
				System.arraycopy(filtered, dataPerBlock, overlapData, 0, this.kernelLength-1);
				
				if(this.outputSize + dataPerBlock >= this.outputBuffer.length) {
					//It is possible to output a full size block
					
					int amountCopiedToOutput =  this.outputBuffer.length - this.outputSize; //Amount of data required to fill one output block
					System.arraycopy(filtered, 0, this.outputBuffer, this.outputSize,amountCopiedToOutput);
					downstreamModule.updateData(this.outputBuffer);
					this.outputSize = 0;
					
					System.arraycopy(filtered, amountCopiedToOutput, this.outputBuffer, 0, dataPerBlock - amountCopiedToOutput );
					this.outputSize += dataPerBlock - amountCopiedToOutput;
				} else {
					System.arraycopy(filtered, 0, this.outputBuffer, this.outputSize, dataPerBlock);
					this.outputSize += dataPerBlock;
				}
				
				
				
			}
			//Finally store any input data that cannot be processed because it is shorter than the FFT size
			System.arraycopy(newValues, (numberOfFFTsToPerform*dataPerBlock)-this.waitingInputDataLength, this.waitingInputData, 0, totalDataLength-(numberOfFFTsToPerform*dataPerBlock));
			this.waitingInputDataLength = totalDataLength-(numberOfFFTsToPerform*dataPerBlock);

		}
		
	}
	
	@Override
	public void setDownstreamModule(IDataConsumer target) {
		downstreamModule = target;
		
	}

	@Override
	public void updateData(double[] newValues) {
        final Complex[] converted = new Complex[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            converted[i] = Complex.ofCartesian(newValues[i], 0);

        }
        this.updateData(converted);
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		waitingInputDataLength = 0;
		for(int i=0;i<this.kernelLength-1;i++) {
			overlapData[i] = Complex.ofCartesian(0, 0);
		}
		this.outputSize=0;
		this.outputBuffer = null;

		if(downstreamModule != null) {
			downstreamModule.connectToData( streamInformation);
		}	
		
		
		
	}

	@Override
	public void disconnectFromData() {
		// TODO Auto-generated method stub
		
	}
	
}