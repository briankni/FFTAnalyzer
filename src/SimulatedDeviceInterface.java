import java.io.IOException;

import org.usb4java.LibUsbException;


/* This can be used for debugging if enabled through a code change.
 * It simulates samples at 406 KHz of a 
 */

public class SimulatedDeviceInterface extends AbstractDeviceInterface {

	private int sampleRate = 406000;
	private long lastUpdateTimeNS;
	
	private int floatToReading(int valueToConvert) {
		return ((valueToConvert & 0xFF) <<16) + (valueToConvert & 0xFF00) + ((valueToConvert & 0xFF0000) >>16);
		
	}
	
	private float generateSample(long elapsedTimeNS) {
		return elapsedTimeNS;
		//100 Khz sine wave
		
	}
	
	@Override
	public boolean connect() throws LibUsbException, Exception {
		lastUpdateTimeNS = System.nanoTime();
		return true;
	}

	@Override
	public byte[] readBytes(int bytesToRead) throws IOException, InterruptedException {
		long currentTimeNS = System.nanoTime();
		
		//Bytes to read has to be a multiple of 32 bits/4 bytes
		
		for(int bytes=0;bytes<bytesToRead;bytes++) {
			
		}
		return null;
	}

	@Override
	public byte[] readAll() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(byte[] message) {

	}

	@Override
	public void disconnect() {

	}

}
