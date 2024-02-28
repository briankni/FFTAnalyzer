import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public abstract class XYDisplay extends AbstractGraph{

	private static final long serialVersionUID = 3825714829330761864L;
	private double trace1Values[] = {};
	private double trace2Values[] = {};
	private int xmin=0, xmax=32768; //X scale in units of data points (not all points may be plotted if this is smaller than data)
	private double ymin=-140, ymax=0; //Y scale as both displayed and labeled on display
	private double xUnitsMin=0, xUnitsMax=100; //X scale as labeled on display
	protected static int X_AXIS_HEIGHT = 50;
	protected static int Y_AXIS_WIDTH = 50;
	private int xticks = 10;
	private int yticks = 10;
	private float xscale=0;
	private float yscale=0;
	
	
	
    public Dimension getPreferredSize(){
        return new Dimension(4096, 125);
    }
    
    protected void setTrace1Values(double[] newValues) {
    	this.trace1Values = newValues;
    }
    
    protected double[] getTrace1Values() {
    	return this.trace1Values;
    }
    
    protected void setTrace2Values(double[] newValues) {
    	this.trace2Values = newValues;
    }
    
    protected double[] getTrace2Values() {
    	return this.trace2Values;
    }
    

    protected float dataIndexToXCoordinate(int i) {
    	return Y_AXIS_WIDTH + this.xscale*i;
    }
    
    protected double independentValueToXCoordinate(double x) {
    	return Y_AXIS_WIDTH + (x/(xUnitsMax-xUnitsMin)) * (xmax-xmin)*xscale;
    }
    
    protected int xCoordinateToDataIndex(float x) {
    	return (int) ((x - Y_AXIS_WIDTH)/this.xscale);
    }
    
    protected double dataValueToYCoordinate(double y) {
    	return  (-1*this.yscale*y+ymax*this.yscale);
    }
    
    
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        double[] valuesToPlotForTrace1 = trace1Values;
        double[] valuesToPlotForTrace2 = trace2Values;
        
        Rectangle bounds = g.getClipBounds();
        if(trace1Values == null || trace1Values.length <1) {
        	return;
        }
        
        //g.drawString(Double.toString(valuesToPlot[0]), Y_AXIS_WIDTH+20, 20);
        
        yscale = (float) ((bounds.height-X_AXIS_HEIGHT)/(ymax-ymin));
        xscale = (float) ((bounds.width-Y_AXIS_WIDTH)/((double)xmax-xmin));
        float xprev = Y_AXIS_WIDTH;
        double yprev =  -1*yscale*valuesToPlotForTrace1[0];
        
       for(int i=1;i<valuesToPlotForTrace1.length;i++) {
    	   float xcurrent = dataIndexToXCoordinate(i);
    	   double ycurrent = dataValueToYCoordinate(valuesToPlotForTrace1[i]);
       	   g2.draw(new Line2D.Double (xprev, yprev , xcurrent, ycurrent));
       	   xprev = xcurrent;
       	   yprev = ycurrent;
           
       }
       
       if(valuesToPlotForTrace2 != null && valuesToPlotForTrace2.length >1) {
    	   xprev = Y_AXIS_WIDTH;
           yprev =  -1*yscale*valuesToPlotForTrace2[0];
           g2.setColor(Color.RED);
	       for(int i=1;i<valuesToPlotForTrace2.length;i++) {
	    	   float xcurrent = dataIndexToXCoordinate(i);
	    	   double ycurrent = dataValueToYCoordinate(valuesToPlotForTrace2[i]);
	       	   g2.draw(new Line2D.Double (xprev, yprev , xcurrent, ycurrent));
	       	   xprev = xcurrent;
	       	   yprev = ycurrent;
	           
	       }
       }
       
       g2.setColor(Color.BLACK);
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
    }

    public Point2D getLocationOf(double xAxisValue, double yAxisValue) {
    	Point2D value = new Point();
    	value.setLocation(independentValueToXCoordinate(xAxisValue), (double) this.dataValueToYCoordinate(yAxisValue));
    	return value;
    }

	public XYDisplay setXmin(int xmin) {
		this.xmin = xmin;
		return this;
	}
	public XYDisplay setXmax(int xmax) {
		this.xmax = xmax;
		return this;
	}
	public XYDisplay setXUnitsMin(double xUnitMin) {
		this.xUnitsMin = xUnitMin;
		return this;
	}
	public XYDisplay setXUnitsMax(double xUnitMax) {
		this.xUnitsMax = xUnitMax;
		return this;
	}
	
	public double getXMin() {
		return this.xmin ;
	}
	public double getXMax() {
		return this.xmax;
	}
	
	public double getYMin() {
		return this.ymin ;
	}
	public double getYMax() {
		return this.ymax;
	}
	
	public XYDisplay setYmin(double ymin) {
		this.ymin = ymin;
		return this;
	}
	public XYDisplay setYmax(double ymax) {
		this.ymax = ymax;
		return this;
	}
	
	public double getXUnitsMax() {
		return this.xUnitsMax;
	}
    
    
}
