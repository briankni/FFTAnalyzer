
import javax.sound.sampled.AudioFormat;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioSink implements IDataConsumer{
	

	
	
	static double defaultMaxValue = 0.4096;
	private int saveBufferLength;
	private double maxValue = AudioSink.defaultMaxValue;
	public double getMaxValue() {
		return maxValue;
	}


	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	private static int playBackBytesPerSample = 2;
	private SourceDataLine outputLine;
	private float sampleRateIn;

	
	AudioSink(int sampleRateIn){
		this.sampleRateIn = sampleRateIn;
		this.saveBufferLength = sampleRateIn*10;
	}

	
	private AudioFormat getPlaybackFormat() {
		return new AudioFormat((float) this.sampleRateIn, 16, 1 /* mono */,true /*Signed*/, true /*Big Endian*/);
	}
	
	public synchronized void playSamples(double[] input) {

		byte[] samples = new byte[input.length*2]; //16-bit resolution
		   int i = 0;
		   

			   for(double d: input) {
				   int scaledValue = (int) ((d/this.maxValue) * Math.pow(2,15));
				   samples[i+1] = (byte) (scaledValue & 0xFF);
				   samples[i] = (byte) (scaledValue>>8 & 0xFF);
						   
				   i +=2;
				   if (i>= input.length*2){
					   break;
				   }
			   }
		   
		//outputLine.flush();
		outputLine.write(samples, 0, samples.length);
	}
	
	

	@Override
	public synchronized void updateData(double[] newValues) {

			this.playSamples(newValues);
			

		
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
        try {
        	AudioFormat format = this.getPlaybackFormat();
        	DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			outputLine = (SourceDataLine) AudioSystem.getLine(info);
			outputLine.open(format, this.saveBufferLength * playBackBytesPerSample);
			outputLine.start();
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	@Override
	public void disconnectFromData() {
		outputLine.close();
		
	}
	
	

}
