

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;


public class EventUpdateThread extends SwingWorker<Integer,StateUpdate>{
	

	

		private AbstractDeviceInterface deviceInterface;
		//GraphDisplay chart;
		FFTAnalyzer client;
		IDataConsumer targetConsumer;
		int bytesPerSample = 3;
		int sampleSpacingBytes = 4; //May be status or CRC byte
		double samplesToTransform[];
		double OFFSET = 0.0;
		double sampleRate = 0.0;
		double dataRangeVolts = 4.096;  
		DataStreamInformation streamInformation;
		
		
		public  EventUpdateThread(AbstractDeviceInterface sensor, IDataConsumer consumer, FFTAnalyzer clientToUse, DataStreamInformation streamInformation) {
			client = clientToUse;
			deviceInterface = sensor;
			targetConsumer = consumer;
			this.sampleRate = (double) streamInformation.getAttribute(DataStreamInformation.SAMPLERATE);
			this.dataRangeVolts = (double) streamInformation.getAttribute(DataStreamInformation.MAXVALUE);
			this.streamInformation = streamInformation;
			
		}
		

		@Override
		protected Integer doInBackground() {
			boolean connected = false;
			while( !this.isCancelled()) {
				
				try {
					if(connected == false) {
						//The data originates from this point, so it follows
						//that the data stream metadata should originate from this point as well
						
						connected = deviceInterface.connect();
						if(connected) {
							targetConsumer.connectToData(this.streamInformation);
						}
						
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
					if(connected == true) {
						targetConsumer.disconnectFromData();
					}
					connected = false;
					try {
						
						deviceInterface.disconnect();
					} catch (Exception ex) {
						System.err.println(ex);
					}
					try {
						Thread.sleep(1000);
						continue;
					} catch (InterruptedException e) {
						//ignore
					}
				} 

				try {
					int transformSize = client.getTransformSize();
					if(samplesToTransform == null || samplesToTransform.length != transformSize) {
						samplesToTransform = new double[transformSize];
					}
					byte[] buffer = deviceInterface.readBytes(transformSize*sampleSpacingBytes);
				
					
					for(int i=0; i<4*transformSize;i+=4) {
						 
						 samplesToTransform[i/4] = (buffer[i+2]<<24>>8) & 0xFFFF0000 |(buffer[i+1]<<8 & 0xFF00) | ((int)buffer[i] & 0xFF);
						 //scaling is +/- 4.096V in +/- 2^23 counts
						 samplesToTransform[i/4] = -4.096*(samplesToTransform[i/4]/(Math.pow(2,23)))-OFFSET;
						 //Now scaled to volts
						 
					}
					targetConsumer.updateData(samplesToTransform);
					
					
					
						
				} catch (Exception e) {
					System.out.println("Read failure");
					if(connected == true) {
						targetConsumer.disconnectFromData();
					}
					deviceInterface.disconnect();
					connected = false;
				}
			}
			return 0;};
		
		
			
		@Override 
		protected void done() {
		
				deviceInterface.disconnect();
				try {
					get();
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.getCause().printStackTrace();

				}

		}
		
		@Override
		protected void process(List<StateUpdate> updates) {
			//Runs in swing main thread to apply updates
			//Throw out any unprocessed intermediate updates
			//StateUpdate latest = updates.get(updates.size()-1); 
			
		};

}
