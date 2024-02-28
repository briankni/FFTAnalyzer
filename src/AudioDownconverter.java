import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class AudioDownconverter extends SpectrumDisplay {

	private static final long serialVersionUID = 2752105762015132541L;
	private double centerFrequency;
	private ComplexMixer mixer;
	private ISignalChainBlock ppf;
	private ISignalChainBlock filter;
	private ISignalChainBlock decimator;
	private AudioSink audioOut;
	private double maxValue;
	private double sampleRate = 1;
	
	AudioDownconverter(){
		this.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				AudioDownconverter.this.processKeyEvent(e.getKeyChar());
				
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
	
		    });

		
		
		this.setFocusable(true);
		
		
		
	}
	
	public void connectToData(DataStreamInformation streamInformation) {
		this.maxValue = (double) streamInformation.getAttribute(DataStreamInformation.MAXVALUE);
		this.sampleRate = (double) streamInformation.getAttribute(DataStreamInformation.SAMPLERATE);
		//Signal processing pipeline 
				mixer = new ComplexMixer(60000.0/this.sampleRate);
				ppf = new PositivePassFilter(new FIRFilter(FIRFilter.hilbertTransformerKernel(200)), 200);
				filter = new FIRFilter(FIRFilter.lowpassFilterKernel(20000.0/this.sampleRate,100));
				decimator = new Decimation(this.sampleRate, this.sampleRate/5);
				audioOut = new AudioSink((int) (this.sampleRate/5));
				mixer.setDownstreamModule(ppf);
				ppf.setDownstreamModule(filter);
				filter.setDownstreamModule(decimator);
				decimator.setDownstreamModule(audioOut);
				this.setCenterFrequency(30000.0);
		super.connectToData(streamInformation);
		this.mixer.connectToData(streamInformation);
	}
	
	public void disconnectFromData() {
		super.disconnectFromData();
		this.mixer.disconnectFromData();
	}
	
    public void updateData(double[] newValues) {
    	super.updateData(newValues);
    	mixer.updateData(newValues);
    	
    }
	
	private void processKeyEvent(char k) {
		switch(k) {
			case 'l':
				this.setCenterFrequency(centerFrequency+1000);
				break;
			case 'j':
				this.setCenterFrequency(centerFrequency-1000);
				break;
			case 'i':
				this.maxValue *=0.5;
				this.audioOut.setMaxValue(this.maxValue);
				break;
			case 'k':
				this.maxValue *=2;
				this.audioOut.setMaxValue(this.maxValue);
				break;
		}
	}
	
	/*
	 * Center frequency is set in Hz.  Scaled by the sample rate of the data stream.
	 */
	public void setCenterFrequency(double f) {
		this.centerFrequency = f;
		//downconvert the center frequency to 10 Khz
		this.mixer.setFrequency(f/this.sampleRate);
		
	}
	
    public void paint(Graphics g) {
    	super.paint(g);
    	Graphics2D g2 = (Graphics2D) g;
        Point2D start = this.getLocationOf(centerFrequency,this.getYMax());
        Point2D end = this.getLocationOf(centerFrequency+20000, this.getYMin());
        g2.setColor(Color.RED);
        g2.draw( new Rectangle2D.Double (start.getX(),start.getY(), end.getX() - start.getX(), end.getY() - start.getY()));
        g2.setColor(Color.BLACK);
        g.drawString("j-Lower F, l-Higher F, i-Volume up, k-Volume down", XYDisplay.Y_AXIS_WIDTH+20, 100);
        
    }
        

}
