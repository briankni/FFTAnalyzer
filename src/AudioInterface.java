import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioInterface implements IDataConsumer{
	

	
	
	int saveBufferLength = 0;
	Deque<double[]> sampleQueue;
	double maxValue = 1;
	int samples = 0;
	static int playBackBytesPerSample = 2;
	private SourceDataLine outputLine;
	int sampleRate=1;

	
	AudioInterface(int saveBufferLengthRequest){
		saveBufferLength = saveBufferLengthRequest;
		sampleQueue = new ArrayDeque<double[]>(saveBufferLength+1);
		
		
		
	}
	
	private AudioFormat getSaveFormat() {
		return new AudioFormat((float) this.sampleRate, 24, 1 /* mono */,true /*Signed*/, true /*Big Endian*/);
	}
	
	private AudioFormat getPlaybackFormat() {
		return new AudioFormat((float) 44100, 16, 1 /* mono */,true /*Signed*/, true /*Big Endian*/);
	}
	

	
	public synchronized void playBuffer() {

		byte[] samples = new byte[saveBufferLength*2];
		   int i = 0;
		   
		   //There may be more retained data than required
		   
		   for(double[] data: sampleQueue) {
			   if (i>= saveBufferLength*2){
				   break;
			   }
			   for(double d: data) {
				   int scaledValue = (int) ((d/maxValue) * Math.pow(2,15));
				   samples[i+1] = (byte) (scaledValue & 0xFF);
				   samples[i] = (byte) (scaledValue>>8 & 0xFF);
						   
				   i +=2;
				   if (i>= saveBufferLength*2){
					   break;
				   }
			   }
		   }
		outputLine.flush();
		outputLine.write(samples, 0, samples.length);
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
	
	
	public void writeToFile(byte[] samples, int bytesPerSample, Path fileToBeWritten) {
		
		
		try {
			ByteArrayInputStream byteStream = new ByteArrayInputStream(samples);
			
            AudioInputStream audioStream = new AudioInputStream(byteStream, getSaveFormat(), (int) samples.length/bytesPerSample);
            int written = AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, fileToBeWritten.toFile());
            if(written < samples.length) {
            	throw new IllegalArgumentException("Wrote " + Integer.toString(written) + " samples out of an expected " + Integer.toString(samples.length));
            }
		} catch (IOException ex) {
			throw new IllegalArgumentException("An error occured during file save '" + fileToBeWritten.toString() + "'", ex);
		}
		
	}
	
	//Currently exports 24 bits per sample wav
	public synchronized void saveBuffer() {
		   DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH mm ss");  
		   LocalDateTime now = LocalDateTime.now();  
		   Path target = FileSystems.getDefault().getPath(now.format(dtf)+".wav");
		   byte[] samples = new byte[saveBufferLength*3];
		   int i = 0;
		   
		   //There may be more retained data than required
		   
		   for(double[] data: sampleQueue) {
			   if (i>= saveBufferLength*3){
				   break;
			   }
			   for(double d: data) {
				   int scaledValue = (int) ((d/maxValue) * Math.pow(2,23));
				   samples[i+2] = (byte) (scaledValue & 0xFF);
				   samples[i+1] = (byte) (scaledValue>>8 & 0xFF);
				   samples[i] = (byte) (scaledValue>>16 & 0xFF);
						   
				   i +=3;
				   if (i>= saveBufferLength*3){
					   break;
				   }
			   }
		   }
		   this.writeToFile(samples, 3, target);
		   
	}

	@Override
	public synchronized void updateData(double[] newValues) {
			//Retain at least the required amount of data
			sampleQueue.addLast(newValues.clone());
			samples += newValues.length;
			while((sampleQueue.peekFirst() != null ) &&(samples - sampleQueue.peekFirst().length) >saveBufferLength ) {
				double[] toRemove = sampleQueue.pop();
				samples -= toRemove.length;
			}
		
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
        try {
        	this.maxValue = (Double) streamInformation.getAttribute(DataStreamInformation.MAXVALUE);
        	this.sampleRate = ((Double)streamInformation.getAttribute(DataStreamInformation.SAMPLERATE)).intValue();
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
