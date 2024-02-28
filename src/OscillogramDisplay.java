import org.apache.commons.numbers.complex.Complex;

public class OscillogramDisplay extends XYDisplay {

    private static final long serialVersionUID = -6212248538556570697L;

	public void updateData(double[] newValues) {
    	this.setTrace1Values(newValues);
    	this.setTrace2Values(null);
    	this.repaint();
    }
	
	public void updateData(Complex[] newValues) {
		double[] realComponent;
		double[] imaginaryComponent;
		realComponent = new double[newValues.length];
		imaginaryComponent = new double[newValues.length];
		for(int i=0;i<newValues.length;i++) {
			realComponent[i] = newValues[i].getReal();
			imaginaryComponent[i] = newValues[i].getImaginary();
		}
		
    	this.setTrace1Values(realComponent);
    	this.setTrace2Values(imaginaryComponent);
    	this.repaint();
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
