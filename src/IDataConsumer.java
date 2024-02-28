import org.apache.commons.numbers.complex.Complex;

public interface IDataConsumer {
	public default void updateData(double[] newValues) {
        final Complex[] cpart = new Complex[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            cpart[i] = Complex.ofCartesian(newValues[i], 0);

        }
        this.updateData(cpart);
	}
	
	public default void updateData(Complex[] newValues) {
        final double[] realPart = new double[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            realPart[i] = newValues[i].getReal();

        }
        this.updateData(realPart);
	}
	
	public void connectToData(DataStreamInformation streamInformation);
	
	public void disconnectFromData();
}
