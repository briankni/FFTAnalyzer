
public class SpeedTest {
	DeviceInterfaceJDUSB connection = new DeviceInterfaceJDUSB();
	
	public void run() {
		try {
			//Warmup
			connection.connect();
			for(int i=0;i<10;i++) {
				connection.readAll();
			}
			
			//Measure
			byte[] data; 
			for(int repeats=1;repeats<=1000;repeats++) {
				long start = System.currentTimeMillis();
				long total=0;
				for(int i=0;i<1000;i++) {
					data = connection.readAll();
					total += data.length;
				}
				long end = System.currentTimeMillis();
				System.out.println("Speed in MB/Sec: " + .001*total/(end-start));
				//Expected speed = 32 bits*1/8 bytes/bit * 406000 samples/sec * = 1.624 MB/s
				
			}	
			connection.disconnect();
		} catch (Exception e) {
			connection.disconnect();
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SpeedTest test = new SpeedTest();
		test.run();

	}

}
