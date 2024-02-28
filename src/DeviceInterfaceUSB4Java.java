

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;


import org.usb4java.*;






public class DeviceInterfaceUSB4Java extends AbstractDeviceInterface {
	private static final short VENDOR_ID = 0x0FFF;

	private static final short PRODUCT_ID = 0x1234;
	
	/** Endpoint to use for communication */
	private static final byte IN_ENDPOINT = (byte)0x81;
	private static final byte OUT_ENDPOINT = (byte)0x2;
	private static final byte INTERFACE = (byte)0x0;
	private static int ENDPOINT_SIZE = 0;

	private static final long TIMEOUT = 1000;
	
	private static final int BUFFER_SIZE = 65536*8;
	
	private DeviceHandle handle;
	
	private EventHandlingThread eventThread;
	
	private PipedOutputStream readBufferIn;
	private PipedInputStream readBufferOut;
	private Context context = null;
	private Device device;

	public enum States {DISCONNECTED,CONNECTED,FAILED};
	private DeviceInterfaceUSB4Java.States state;
	private PropertyChangeSupport pcSupport;
	
	public DeviceInterfaceUSB4Java() {
		this.state = States.DISCONNECTED;
		pcSupport = new PropertyChangeSupport(this);
	}
	
	public void addListener(PropertyChangeListener listener) {
		pcSupport.addPropertyChangeListener(listener);
	}
	
	private void setConnectionState(States newState) {
		synchronized(this.state) {
			DeviceInterfaceUSB4Java.States oldState = this.state;
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
	            int result = LibUsb.handleEventsTimeout(null, 500000);
	            if (result != LibUsb.SUCCESS && state==States.CONNECTED) {
	            	setConnectionState(States.FAILED);
	                throw new LibUsbException("Unable to handle events", result);
	            }	
	        }
	    }
	}
	
	private Device findDevice(short vendorId, short productId)
	{
	    // Read the USB device list
	    DeviceList list = new DeviceList();
	    int result = LibUsb.getDeviceList(context, list);
	    if (result < 0) throw new LibUsbException("Unable to get device list", result);

	    try
	    {
	        // Iterate over all devices and scan for the right one
	        for (Device device: list)
	        {
	            DeviceDescriptor descriptor = new DeviceDescriptor();
	            result = LibUsb.getDeviceDescriptor(device, descriptor);
	            if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to read device descriptor", result);
	            if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) return device;
	        }
	    }
	    finally
	    {
	        // Ensure the allocated device list is freed
	        LibUsb.freeDeviceList(list, true);
	    }

	    // Device not found
	    return null;
	}
	
    
    private final TransferCallback dataReceived = new TransferCallback()
    {
        @Override
        public void processTransfer(Transfer transfer)
        {
        	int len = transfer.buffer().remaining();
            try {
            	if(transfer.buffer().hasArray()) {
            		readBufferIn.write(transfer.buffer().array(),0,len);
            	} else {
            		byte[] bytes = new byte[len];
            		transfer.buffer().get(bytes);
            		if (readBufferOut.available() + len >= BUFFER_SIZE) {
            			System.out.println("Buffer full - data lost");
            		} else {
            	
	            		readBufferIn.write(bytes,0,len);
	            		synchronized(readBufferOut) {
	            			readBufferOut.notify();
	            		}
            		}
            	}
			} catch (IOException e) {
				//Throw away unsaved data
				e.printStackTrace();
			}
            LibUsb.freeTransfer(transfer);
            if(eventThread.abort == false) {
            	//larger reads appear to be less likely to stall the USB bus due to the host
            	read(handle,8*ENDPOINT_SIZE,dataReceived);
            }
            
        }
    };

    
    /**
     * Asynchronously reads some data from the device.
     * 
     * @param handle
     *            The device handle.
     * @param size
     *            The number of bytes to read from the device.
     * @param callback
     *            The callback to execute when data has been received.
     */
    private void read(DeviceHandle handle, int size,
        TransferCallback callback)
    {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(size).order(
            ByteOrder.LITTLE_ENDIAN);
        Transfer transfer = LibUsb.allocTransfer();
        LibUsb.fillBulkTransfer(transfer, handle, IN_ENDPOINT, buffer,
            callback, null, TIMEOUT);
        int result = LibUsb.submitTransfer(transfer);
        if (result != LibUsb.SUCCESS)
        {
        	this.setConnectionState(States.FAILED);
        	System.err.println("Unable to submit transfer");
            //throw new LibUsbException("Unable to submit transfer", result);
            
        }
    }
	
	public boolean connect() throws LibUsbException, Exception {
		
		//Buffer for streaming data
		readBufferIn = new PipedOutputStream();
		readBufferOut = new PipedInputStream(readBufferIn, BUFFER_SIZE);
		
		context = new Context();
        int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS)
        {
            throw new LibUsbException("Unable to initialize libusb", result);
        }
        
        device = findDevice( VENDOR_ID,  PRODUCT_ID);
        if (device == null) throw new Exception("Unable to find device");
        
        handle = new DeviceHandle();
        result = LibUsb.open(device, handle);
        
        //handle = LibUsb.openDeviceWithVidPid(context,
        //        VENDOR_ID, PRODUCT_ID);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", result);

        if (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)) {
        	 // Check if kernel driver is attached to the interface
            int attached = LibUsb.kernelDriverActive(handle, IN_ENDPOINT);
            if (attached < 0)
            {
                throw new LibUsbException(
                    "Unable to check kernel driver active", result);
            }

            // Detach kernel driver from interface 0 and 1. 
            //Interface 0 is for configuration, 1 is for CDC class
            if(attached > 0) {
	            result = LibUsb.detachKernelDriver(handle, INTERFACE);
	            if (result != LibUsb.SUCCESS &&
	                result != LibUsb.ERROR_NOT_SUPPORTED &&
	                result != LibUsb.ERROR_NOT_FOUND)
	            {
	                throw new LibUsbException("Unable to detach kernel driver",
	                    result);
            }
            }
        }
        
            // Claim interface
            result = LibUsb.claimInterface(handle, INTERFACE);
            if (result != LibUsb.SUCCESS)
            {
                throw new LibUsbException("Unable to claim interface", result);
            }
            eventThread = new EventHandlingThread();
            eventThread.setPriority(10);
            eventThread.start();
            
            ///Queue up three requests
            //Hope is one is receiving, one is finishing and one is waiting
            this.setConnectionState(States.CONNECTED);
            
          
            ENDPOINT_SIZE =  LibUsb.getMaxPacketSize(device, IN_ENDPOINT);
            read(handle, ENDPOINT_SIZE, dataReceived);
            read(handle, ENDPOINT_SIZE, dataReceived);
            read(handle, ENDPOINT_SIZE, dataReceived);
            read(handle, ENDPOINT_SIZE, dataReceived);
            read(handle, ENDPOINT_SIZE, dataReceived);


            
            
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
		ByteBuffer buffer = BufferUtils.allocateByteBuffer(message.length);
        buffer.put(message);
        buffer.rewind();
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int result = LibUsb.bulkTransfer(handle, OUT_ENDPOINT, buffer,
            transferred, TIMEOUT);
        if (result != LibUsb.SUCCESS)
        {
        	this.state  = States.FAILED;
            throw new LibUsbException("Unable to send data", result);
            
        }
        System.out.println(transferred.get() + " bytes sent to device");
	}
	
	public synchronized void disconnect() {
		
		if(this.state == States.DISCONNECTED)
			return;
		
		try {
			if(eventThread != null) {
				eventThread.abort();
				try {
					eventThread.join();
				} catch (InterruptedException e) {
					System.out.println("The event thread may not have finished");
				}
			}
			if(handle != null) {
			    int result = LibUsb.releaseInterface(handle, INTERFACE);
			    if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to release interface", result);
			}
		}
        finally
        {
            if(handle != null)
            	LibUsb.close(handle);
            	handle = null;
            if(context != null) {
            	LibUsb.exit(context);
            	context = null;
            }
            this.state = States.DISCONNECTED;
            synchronized(readBufferOut) {
    			readBufferOut.notify();
    		}
        }

        
	}
	

	
}
