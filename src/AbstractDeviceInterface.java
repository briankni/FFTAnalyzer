import java.io.IOException;

import org.usb4java.LibUsbException;

public abstract class AbstractDeviceInterface {

	public abstract boolean connect() throws LibUsbException, Exception;
	
	public abstract  byte[] readBytes(int bytesToRead) throws IOException, InterruptedException;
	
	public abstract byte[] readAll() throws IOException, InterruptedException;
	
	public abstract void write(byte[] message);
	
	public abstract void disconnect();
	
}
