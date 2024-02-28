import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import org.apache.commons.math4.transform.FastFourierTransform;
import org.apache.commons.numbers.complex.Complex;

public class WaterfallDisplay extends AbstractGraph{

	private static final long serialVersionUID = 3825714829330761864L;
	private double values[] = {};
	private int xmin=0, xmax=32768; //X scale in units of data points (not all points may be plotted if this is smaller than data)
	private double ymin=-140, ymax=0; //Y scale as both displayed and labeled on display
	private double colormin=-100, colormax=0;  
	private double xUnitsMin=0, xUnitsMax=100; //X scale as labeled on display
	private static int X_AXIS_HEIGHT = 50;
	private static int Y_AXIS_WIDTH = 50;
	private int xticks = 10;
	private int yticks = 10;
	private double magnitude[];
	private int transformSize = 2048;
	private BufferedImage oldBuffer;
	private BufferedImage newBuffer;
	private int verticalResolution = 800, horizontalResolution=2048;
	private FastFourierTransform transform = new FastFourierTransform(FastFourierTransform.Norm.STD);
	
	
	
	WaterfallDisplay(){
		oldBuffer = new BufferedImage(horizontalResolution,verticalResolution,BufferedImage.TYPE_3BYTE_BGR);
		
		newBuffer = new BufferedImage(horizontalResolution,verticalResolution,BufferedImage.TYPE_3BYTE_BGR);
	}
	
    public Dimension getPreferredSize(){
        return new Dimension(horizontalResolution, verticalResolution);
    }
    
    private Color colorMap(double value) {
    	return Color.getHSBColor((float) ((value - colormin)*(1/(colormax-colormin))),1.0f,1.0f);
    }
    
    public void updateData(double[] newValues) {
    	
    	values = newValues.clone();
    	
    	transformSize = newValues.length;
    	magnitude = new double[transformSize/2];
    	
    	//Apply Hann window
    	for(int i=0;i<transformSize;i++) {
    		values[i] *= 2*0.5*(1-Math.cos((2*Math.PI*i)/transformSize));
    	}
    	
    	try {           
	        Complex[] complx = transform.apply(values);

	        for (int i = 0; i < (complx.length/2); i++) { 
	            double rr = (complx[i].getReal());
	            double ri = (complx[i].getImaginary());

	    
	            if(ri == 0.0 || rr == 0.0) {
	            	magnitude[i] = 0.0;
	            	continue;
	            }
	            
	            magnitude[i] = Math.sqrt((rr * rr) + (ri * ri)) ;
	           
	            magnitude[i] = 20*Math.log10(1/((float)transformSize)*magnitude[i]) ;
	        }
	 
	        
	    } catch (IllegalArgumentException e) {
	        System.out.println(e);
	    }
    	
    	Graphics2D g2d = newBuffer.createGraphics();
    	
    	//Shift image down 1 row
    	g2d.drawImage(oldBuffer,0,1,null);
    	
    	//Draw new row
    	double pixelWidth = (double)horizontalResolution/magnitude.length;
    	for(int i=xmin;i<Math.min(magnitude.length, xmax);i++) {
    		
    		double xcurrent =  i*pixelWidth;
    		g2d.setColor(this.colorMap(magnitude[i]));
    		g2d.draw(new Line2D.Double (xcurrent, 1, xcurrent+pixelWidth,1));
            
         }
    	
    	
    	BufferedImage temp = oldBuffer;
    	oldBuffer = newBuffer;
    	newBuffer = temp;
    	this.repaint();
    }
    
    
    
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = g.getClipBounds();
        
        float yscale = (float) ((bounds.height-X_AXIS_HEIGHT)/(ymax-ymin));
        float xscale = (float) ((bounds.width-Y_AXIS_WIDTH)/((double)xmax-xmin));
        
      //Draw x-axis
        g2.drawLine(0, bounds.height-X_AXIS_HEIGHT, bounds.width, bounds.height-X_AXIS_HEIGHT);
        for(int i=0;i<=this.xticks;i++) {
     	   g2.draw(new Line2D.Double (Y_AXIS_WIDTH + i*xscale*(xmax-xmin)/xticks, bounds.height-X_AXIS_HEIGHT , X_AXIS_HEIGHT + i*xscale*(xmax-xmin)/xticks, bounds.height-X_AXIS_HEIGHT+10));
     	   g2.drawString(Double.toString(i*(xUnitsMax-xUnitsMin)/xticks + xUnitsMin), (int) (Y_AXIS_WIDTH + i*xscale*(xmax-xmin)/xticks)-30, (int)bounds.height-(X_AXIS_HEIGHT/2) );
        }
        
        
        //Draw y Axis
        g2.drawLine(Y_AXIS_WIDTH, 0, Y_AXIS_WIDTH, bounds.height);
        for(int i=0;i<=this.yticks;i++) {
     	   g2.draw(new Line2D.Double (Y_AXIS_WIDTH/2, (bounds.height-X_AXIS_HEIGHT) -(i*yscale*(ymax-ymin)/yticks),Y_AXIS_WIDTH, (bounds.height-X_AXIS_HEIGHT) -(i*yscale*(ymax-ymin)/yticks) ));
     	   g2.drawString(Double.toString(i*(ymax-ymin)/xticks + ymin), 0,(int)( (bounds.height-X_AXIS_HEIGHT) -(i*yscale*(ymax-ymin)/yticks)));
        }
        
        //Paste the cumulative image
        g2.drawImage(oldBuffer, Y_AXIS_WIDTH, 0,bounds.width-Y_AXIS_WIDTH,bounds.height-X_AXIS_HEIGHT, null);
    }


	public WaterfallDisplay setXmin(int xmin) {
		this.xmin = xmin;
		return this;
	}
	public WaterfallDisplay setXmax(int xmax) {
		this.xmax = xmax;
		return this;
	}
	public WaterfallDisplay setXUnitsMin(double xUnitMin) {
		this.xUnitsMin = xUnitMin;
		return this;
	}
	public WaterfallDisplay setXUnitsMax(double xUnitMax) {
		this.xUnitsMax = xUnitMax;
		return this;
	}
	public WaterfallDisplay setYmin(double ymin) {
		this.ymin = ymin;
		return this;
	}
	public WaterfallDisplay setYmax(double ymax) {
		this.ymax = ymax;
		return this;
	}

	@Override
	public void saveCSV() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnectFromData() {
		// TODO Auto-generated method stub
		
	}
    
    
}
