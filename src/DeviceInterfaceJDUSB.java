

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Optional;

import net.codecrete.usb.USB;
import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBDirection;
import net.codecrete.usb.USBEndpoint;


public class DeviceInterfaceJDUSB extends AbstractDeviceInterface {
	private static final short VENDOR_ID = 0x0FFF;

	private static final short PRODUCT_ID = 0x1234;
	
	/** Endpoint to use for communication */
	private static final byte IN_ENDPOINT = (byte)0x1;
	private static final byte OUT_ENDPOINT = (byte)0x2;
	private static final byte INTERFACE = (byte)0x0;
	private static int ENDPOINT_SIZE = 0;

	private static final int TIMEOUT = 1000; //In milliseconds
	
	private static final int BUFFER_SIZE = 65536*8;
	
	private USBDevice device;
	
	private EventHandlingThread eventThread;
	
	private PipedOutputStream readBufferIn;
	private PipedInputStream readBufferOut;

	public enum States {DISCONNECTED,CONNECTED,FAILED};
	private DeviceInterfaceJDUSB.States state;
	private PropertyChangeSupport pcSupport;
	
	public DeviceInterfaceJDUSB() {
		this.state = States.DISCONNECTED;
		pcSupport = new PropertyChangeSupport(this);
	}
	
	public void addListener(PropertyChangeListener listener) {
		pcSupport.addPropertyChangeListener(listener);
	}
	
	private void setConnectionState(States newState) {
		synchronized(this.state) {
			DeviceInterfaceJDUSB.States oldState = this.state;
			this.state = newState;
			pcSupport.firePropertyChange("Connection State", oldState, newState );
		}
		
	}
	
	private class EventHandlingThread extends Thread
	{
	    /** If thread should abort. */
	    private volatile boolean abort;

	    /**
	     * Aborts the event handling thread.
	     */
	    public void abort()
	    {
	        this.abort = true;
	    }

	    @Override
	    public void run()
	    {
	        while (!this.abort)
	        {
	        		try {
	        		byte[] buffer = DeviceInterfaceJDUSB.this.device.transferIn(IN_ENDPOINT,(int) TIMEOUT);
	        		if(buffer.length > 0) {
	            		readBufferIn.write(buffer,0,buffer.length);
	            		synchronized(readBufferOut) {
	            			readBufferOut.notify();
	            		}
	            	} 
	        		
	        		} catch (Exception e) {
	        			e.printStackTrace();
	        			setConnectionState(States.FAILED);
	        			this.abort();
	        			System.err.println("Device input is stopped pending reconnection");
	        		}
	        }
	    }
	}
	
	private USBDevice findDevice(short vendorId, short productId)
	{
	   Optional<USBDevice> tempDevice = USB.getDevice(VENDOR_ID, PRODUCT_ID);
	   if(tempDevice.isEmpty()) {
		   return null;
	   }
	   USBDevice device = tempDevice.get();
	   
	   device.open();
	   device.claimInterface(INTERFACE);
	   if(device.isOpen()) {
		   return device;
	   } else {
		   device.close();
		   return null;
	   }
	   
	   
	}
	
	
	public boolean connect() throws Exception {
		
		//Buffer for streaming data
		readBufferIn = new PipedOutputStream();
		readBufferOut = new PipedInputStream(readBufferIn, BUFFER_SIZE);
		
		device = findDevice( VENDOR_ID,  PRODUCT_ID);
		if(device == null) {
			System.out.println("Device not found");
			return false;
		}
        this.setConnectionState(States.CONNECTED);
        USBEndpoint endPoint = this.device.getEndpoint(USBDirection.IN, IN_ENDPOINT);
        if(endPoint == null) {
        	return false;
        }
        ENDPOINT_SIZE =  endPoint.packetSize();
        eventThread = new EventHandlingThread();
        eventThread.setPriority(10);
        eventThread.start();         
        return true;
	}
	
	public synchronized byte[] readBytes(int bytesToRead) throws IOException, InterruptedException {
		int available;
		synchronized(readBufferOut) {
			while((available=this.readBufferOut.available())<bytesToRead && this.state == States.CONNECTED) {
				readBufferOut.wait(TIMEOUT);};
		}
		if(this.state != States.CONNECTED) {
			throw new IOException("Device is disconnected");
		}
		byte[] b = new byte[available];
		this.readBufferOut.read(b, 0, available);
		return b;
	}
	
	public synchronized byte[] readAll() throws IOException, InterruptedException {
		int available;
		synchronized(readBufferOut) {
			while((available=this.readBufferOut.available())<ENDPOINT_SIZE && this.state == States.CONNECTED) {
				readBufferOut.wait(TIMEOUT);};
		}
		if (this.state != States.CONNECTED){
			throw new IOException("Device is disconnected");
		}
		return readBytes(available);
	}
	
	public synchronized void write(byte[] message) {
        this.device.transferOut(OUT_ENDPOINT, message);
        System.out.println(message.length + " bytes sent to device");
	}
	
	public synchronized void disconnect() {
		
		if(this.state == States.DISCONNECTED) {
			return;
		}
		
		try {
			if(eventThread != null) {
				eventThread.abort();
				try {
					eventThread.join();
				} catch (InterruptedException e) {
					System.out.println("The event thread may not have finished");
				}
			}
			this.device.close();
		}
        finally {
        	this.state = States.DISCONNECTED;
        	synchronized(readBufferOut) {
    			readBufferOut.notify();
    		}
            
        }

        
	}
	

	
}
