import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.math4.transform.FastFourierTransform;
import org.apache.commons.numbers.complex.Complex;

public class SpectrumDisplay extends XYDisplay {
	


	private static final long serialVersionUID = 3145367659808068827L;
	
	private int transformSize = 65536;
	private FastFourierTransform transform = new FastFourierTransform(FastFourierTransform.Norm.STD);
	private int mouseX = 0;
	//private int mouseY = 0;
	private IDataConsumer spectrumProcessor;
	
	SpectrumDisplay(){
		super();
		this.addMouseMotionListener(new MouseMotionAdapter() {
		      public void mouseMoved(MouseEvent me)
		      {
		        SpectrumDisplay.this.mouseX = me.getX();
		        //SpectrumDisplay.this.mouseY = me.getY();
		      }
		    });
		
		this.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				SpectrumDisplay.this.requestFocusInWindow();
				
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

		    });

		
	}
	
    public void updateData(double[] newValues) {
    	double magnitude[];

    	double[] processedValues = newValues.clone();
    	transformSize = processedValues.length;
    	magnitude = new double[transformSize/2];
    	
    	//Apply Hann window
    	for(int i=0;i<transformSize;i++) {
    		processedValues[i] *= 2*0.5*(1-Math.cos((2*Math.PI*i)/transformSize));
    	}
    	
    	try {           
	        Complex[] complx = transform.apply(processedValues);

	        for (int i = 0; i < (complx.length/2); i++) { 
	            double rr = (complx[i].getReal());
	            double ri = (complx[i].getImaginary());
	            
	            if(i==0) {
	            	magnitude[i] = Math.sqrt((rr * rr) + (ri * ri));
	            } else {
	            	magnitude[i] = 2*Math.sqrt((rr * rr) + (ri * ri));
	            }
	        }
	        
	        
	            
	         for (int i = 0; i < (complx.length/2); i++) {  
	        	 
	        	 magnitude[i] = this.magnitudeTodBVperSqrtHz(magnitude[i]);
        
	        }
	        this.spectrumProcessor.updateData(magnitude);
	        
	    } catch (Exception e) {
	        System.out.println(e);
	    }
    	
    	
    	//this.repaint();
    }
    
    public void updateData(Complex[] newValues) {
    	double magnitude[];

    	Complex[] processedValues = newValues.clone();
    	transformSize = processedValues.length;
    	magnitude = new double[transformSize/2];
    	
    	//Apply Hann window
    	for(int i=0;i<transformSize;i++) {
    		processedValues[i]= processedValues[i].multiply(2*0.5*(1-Math.cos((2*Math.PI*i)/transformSize)));
    	}
    	
    	try {           
	        Complex[] complx = transform.apply(processedValues);

	        for (int i = 0; i < (complx.length/2); i++) { 
	            double rr = (complx[i].getReal());
	            double ri = (complx[i].getImaginary());
	            
	            if(i==0) {
	            	magnitude[i] = Math.sqrt((rr * rr) + (ri * ri));
	            } else {
	            	magnitude[i] = 2*Math.sqrt((rr * rr) + (ri * ri));
	            }
	        }
	        
	        
	            
	         for (int i = 0; i < (complx.length/2); i++) {  
	        	 
	        	 magnitude[i] = this.magnitudeTodBVperSqrtHz(magnitude[i]);
        
	        }
	        this.spectrumProcessor.updateData(magnitude);
	        
	    } catch (Exception e) {
	        System.out.println(e);
	    }
    	
    	
    	//this.repaint();
    }
    

    
    private double magnitudeTodBVperSqrtHz(double magnitude) {
    	double noiseBinWidth = 1.5 * this.getXUnitsMax()/(transformSize/2);
    	
        return 20*Math.log10((magnitude/(Math.sqrt(2)*transformSize))*Math.sqrt(1/(noiseBinWidth))) ;
    }
    
    private double dBVperSqrtHzToMagnitude(double dBVperSqrtHz) {
    	double noiseBinWidth = 1.5 * this.getXUnitsMax()/(transformSize/2);
    	return (Math.pow(10,dBVperSqrtHz/20)*Math.sqrt(2)*transformSize/Math.sqrt(1/(noiseBinWidth)));
    	
    }
    
    private double magnitudeTodBm(double magnitude) {
    	return this.mWTodBm(this.magnitudeTomW(magnitude)); 
    }
    
    private double magnitudeTomW(double magnitude) {
    	return 1000*Math.pow(magnitude/transformSize,2)/50;
    }
    
    private double mWTodBm(double mw) {
    	return 10*Math.log10(mw);
    }
    
    public void paint(Graphics g) {
    	super.paint(g);
    	
    	double dBmOneBin=0;
    	double dBmSineWave=0;
    	
    	//Single line power
        int j = this.xCoordinateToDataIndex(this.mouseX);
        if(j>=0) {
        	dBmOneBin = this.magnitudeTodBm(this.dBVperSqrtHzToMagnitude(this.getTrace1Values()[j]));
        }
        if(j>=3) {
        	double power = this.magnitudeTomW(this.dBVperSqrtHzToMagnitude(this.getTrace1Values()[j-3]));
        		for(int i=-2;i<=3;i++) {
        			power += this.magnitudeTomW(this.dBVperSqrtHzToMagnitude(this.getTrace1Values()[j+i]));
        		}
        		
            	
            	dBmSineWave = this.mWTodBm(power);
     
	    	int y = (int) this.dataValueToYCoordinate(this.getTrace1Values()[j]);
	    	g.fillOval(this.mouseX-3, y-3, 6, 6);
	    	g.drawString("dBv/sqrt(Hz)="+Double.toString(this.getTrace1Values()[j]), XYDisplay.Y_AXIS_WIDTH+20, 20);
	    	g.drawString("Sine wave dBm="+Double.toString(dBmSineWave), XYDisplay.Y_AXIS_WIDTH+20, 40);
	    	g.drawString("bin dBm="+Double.toString(dBmOneBin), XYDisplay.Y_AXIS_WIDTH+20, 60);


	    	
    	}
    }
    
    

	@Override
	public void saveCSV() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH mm ss");  
		LocalDateTime now = LocalDateTime.now();  
		Path target = FileSystems.getDefault().getPath(now.format(dtf)+".csv"); 
		target.toFile();
		
		double[] values = this.getTrace1Values().clone();
		
		try (FileWriter fw = new FileWriter(target.toFile());
		         BufferedWriter bw = new BufferedWriter(fw)) {
				bw.write("Frequency(Hz),Spectral Density (dBv/sqrt(Hz)");
				bw.newLine();
				for(int i = 0; i<values.length; i++) {
					double f = this.getXUnitsMax()*((double)i/(values.length-1));
					bw.write(Double.toString(f)+","+Double.toString(values[i]));
					bw.newLine();
				}
			
		 } catch (IOException e) {
			
			e.printStackTrace();
		}
		
		
		
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		spectrumProcessor = new Averager(10,new IDataConsumer() {

			@Override
			public void updateData(double[] newValues) {
				SpectrumDisplay.this.setTrace1Values(newValues);
				SpectrumDisplay.this.repaint();
				
			}

			@Override
			public void connectToData(DataStreamInformation streamInformation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void disconnectFromData() {
				// TODO Auto-generated method stub
				
			}} ) ;
		
	}

	@Override
	public void disconnectFromData() {
		// TODO Auto-generated method stub
		
	}
}
