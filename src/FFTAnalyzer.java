import java.awt.EventQueue;

import javax.swing.JFrame;



import java.awt.BorderLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.apache.commons.numbers.complex.Complex;



public class FFTAnalyzer implements IDataConsumer {

	private JFrame frame;
	private static FFTAnalyzer window;
	private EventUpdateThread eventThread;
	private AbstractDeviceInterface usbInterface;
	/**
	 * @wbp.nonvisual location=188,171
	 */
	private AbstractGraph currentDisplay;
	private AudioInterface audioInterface;
	private JMenuBar menuBar;
	private JMenu mnDisplayMenu;
	private JMenuItem mntmWaterfallMenuItem;
	private JMenuItem mntmSpectrumMenuItem;
	
	private int transformSize = 2048; //In samples;
	private JMenuItem mntmOscillogramMenuItem;
	private JMenu mnSamplesMenu;
	private JMenuItem mntm2048MenuItem;
	private JMenuItem mntm4096MenuItem;
	private JMenuItem mntm8192MenuItem_2;
	private JMenuItem mntm16384MenuItem_3;
	private JMenuItem mntm32768MenuItem;
	private JMenuItem mntm65536MenuItem;
	private DataStreamInformation assumedStreamInformation;
	






	private final class PlayBufferAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("PLay Buffer selected.");
			FFTAnalyzer.this.audioInterface.playBuffer();
		}
	}

	private final class SaveCSVAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			FFTAnalyzer.this.currentDisplay.saveCSV();
		}
	}

	private final class SaveGraphAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			FFTAnalyzer.this.currentDisplay.saveImage();
		}
	}

	private enum display {OSCILLOGRAM,SPECTRUM,WATERFALL,AUDIODOWNCONVERTER}; 
	private display displayType;
	private JMenu mnCollectionMenu;
	private JMenuItem mntmSaveRecentMenuItem;
	private JMenuItem mntmSaveGraphMenuItem;
	private JMenuItem mntmSaveCSVMenuItem_1;
	private JMenuItem mntmPlayBufferMenuItem_1;
	private JMenuItem mntmAudioDownconverterMenuItem;
	private DataStreamInformation streamInformation;
	
	
	

    

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new FFTAnalyzer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	/**
	 * Create the application.
	 */
	public FFTAnalyzer() {
		initialize();
		usbInterface = new DeviceInterfaceJDUSB();
		assumedStreamInformation = new DataStreamInformation(406000, -4.096, 4.096);
		
		eventThread = new EventUpdateThread(usbInterface,this, this,this.assumedStreamInformation);
		//eventThread = new EventUpdateThread(usbInterface,this, this);
		
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		mnDisplayMenu = new JMenu("Display");
		menuBar.add(mnDisplayMenu);
		
		mntmWaterfallMenuItem = new JMenuItem("Waterfall Display");
		mntmWaterfallMenuItem.addActionListener(new WaterfallDisplayAction());
		mnDisplayMenu.add(mntmWaterfallMenuItem);
		
		mntmSpectrumMenuItem = new JMenuItem("Spectrum Display");
		mntmSpectrumMenuItem.addActionListener(new SpectrumDisplayAction());
		mnDisplayMenu.add(mntmSpectrumMenuItem);
		
		mntmOscillogramMenuItem = new JMenuItem("Oscillogram");
		mntmOscillogramMenuItem.addActionListener(new OscillogramDisplayAction());
		
		mntmAudioDownconverterMenuItem = new JMenuItem("Audio Downconverter");
		mntmAudioDownconverterMenuItem.addActionListener(new AudioDownconverterDisplayAction());
		mnDisplayMenu.add(mntmAudioDownconverterMenuItem);
		mntmOscillogramMenuItem.setActionCommand("Oscillogram");
		mnDisplayMenu.add(mntmOscillogramMenuItem);
		
		mnSamplesMenu = new JMenu("Samples");
		menuBar.add(mnSamplesMenu);
		
		mntm2048MenuItem = new JMenuItem("2048");
		mntm2048MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformSize = 2048;
				FFTAnalyzer.this.initializeDisplay();
			}
		});
		mntm2048MenuItem.setActionCommand("2048");
		mnSamplesMenu.add(mntm2048MenuItem);
		
		mntm4096MenuItem = new JMenuItem("4096");
		mntm4096MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformSize = 4096;
				FFTAnalyzer.this.initializeDisplay();
			}
		});
		mnSamplesMenu.add(mntm4096MenuItem);
		
		mntm8192MenuItem_2 = new JMenuItem("8192");
		mntm8192MenuItem_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformSize = 8192;
				FFTAnalyzer.this.initializeDisplay();
			}
		});
		mnSamplesMenu.add(mntm8192MenuItem_2);
		
		mntm16384MenuItem_3 = new JMenuItem("16384");
		mntm16384MenuItem_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformSize = 16384;
				FFTAnalyzer.this.initializeDisplay();
			}
		});
		mnSamplesMenu.add(mntm16384MenuItem_3);
		
		mntm32768MenuItem = new JMenuItem("32768");
		mntm32768MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformSize = 32768;
				FFTAnalyzer.this.initializeDisplay();
			}
		});
		mnSamplesMenu.add(mntm32768MenuItem);
		
		mntm65536MenuItem = new JMenuItem("65536");
		mntm65536MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformSize = 65536;
				FFTAnalyzer.this.initializeDisplay();
			}
		});
		mnSamplesMenu.add(mntm65536MenuItem);
		
		mnCollectionMenu = new JMenu("Collection");
		menuBar.add(mnCollectionMenu);
		
		mntmSaveRecentMenuItem = new JMenuItem("Save Recent");
		mntmSaveRecentMenuItem.addActionListener(new SaveRecentAction());
		mnCollectionMenu.add(mntmSaveRecentMenuItem);
		
		mntmSaveGraphMenuItem = new JMenuItem("Save Graph");
		mntmSaveGraphMenuItem.addActionListener(new SaveGraphAction());
		mnCollectionMenu.add(mntmSaveGraphMenuItem);
		
		mntmSaveCSVMenuItem_1 = new JMenuItem("Save CSV");
		mntmSaveCSVMenuItem_1.addActionListener(new SaveCSVAction());
		mnCollectionMenu.add(mntmSaveCSVMenuItem_1);
		
		mntmPlayBufferMenuItem_1 = new JMenuItem("Play Buffer");
		mntmPlayBufferMenuItem_1.addActionListener(new PlayBufferAction());
		mnCollectionMenu.add(mntmPlayBufferMenuItem_1);
		eventThread.execute();

		
		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
	
			@Override
			public void windowClosing(WindowEvent e) {
				FFTAnalyzer.this.eventThread.cancel(true);
			}
		});
		
		this.displayType = FFTAnalyzer.display.WATERFALL;
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		currentDisplay = new WaterfallDisplay();
		frame.getContentPane().add(currentDisplay);
		frame.pack();
		
		
		
	}

	private final class AudioDownconverterDisplayAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("Audio Downconverter display selected.");
			FFTAnalyzer.this.displayType = FFTAnalyzer.display.AUDIODOWNCONVERTER;
			FFTAnalyzer.this.initializeDisplay();
		}
	}
	
	private final class OscillogramDisplayAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("Oscillogram display selected.");
			FFTAnalyzer.this.displayType = FFTAnalyzer.display.OSCILLOGRAM;
			FFTAnalyzer.this.initializeDisplay();
		}
	}

	private final class SpectrumDisplayAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("Spectrum display selected.");
			FFTAnalyzer.this.displayType = FFTAnalyzer.display.SPECTRUM;
			FFTAnalyzer.this.initializeDisplay();
			
		}
	}

	private final class WaterfallDisplayAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("Waterfall display selected.");
			FFTAnalyzer.this.displayType = FFTAnalyzer.display.WATERFALL;
			FFTAnalyzer.this.initializeDisplay();	
			
		}
	}
	
	private final class SaveRecentAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("Save Recent selected.");
			FFTAnalyzer.this.audioInterface.saveBuffer();
		}
	}
	
	public IGraph getChart() {
		return this.currentDisplay;
	}

	public int getTransformSize() {
		return transformSize;
	}
	
	private synchronized void initializeDisplay() {
		frame.getContentPane().remove(currentDisplay);
		currentDisplay.disconnectFromData();
		int dataRate = ((Double)this.streamInformation.getAttribute(DataStreamInformation.SAMPLERATE)).intValue();
		switch(this.displayType) {
			case OSCILLOGRAM:
				currentDisplay = new OscillogramDisplay();
				currentDisplay.setXmin(0).setXmax(getTransformSize()).setYmin(-5).setYmax(5);
				currentDisplay.setXUnitsMax(getTransformSize());
				break;
			case SPECTRUM:
				currentDisplay = new SpectrumDisplay();
				currentDisplay.setXmin(0).setXmax(FFTAnalyzer.this.getTransformSize()/2).setYmin(-150).setYmax(0);
				currentDisplay.setXUnitsMax(dataRate/2);
				break;
			case AUDIODOWNCONVERTER:
				currentDisplay = new AudioDownconverter();
				currentDisplay.setXmin(0).setXmax(FFTAnalyzer.this.getTransformSize()/2).setYmin(-150).setYmax(0);
				currentDisplay.setXUnitsMax(dataRate/2);
				
				break;
			case WATERFALL:
				currentDisplay = new WaterfallDisplay();
				currentDisplay.setXmin(0).setXmax(FFTAnalyzer.this.getTransformSize()/2).setYmin((transformSize*800)/dataRate).setYmax(0);
				currentDisplay.setXUnitsMax(dataRate/2);
				break;
				
		}
		currentDisplay.connectToData(this.streamInformation);
		frame.getContentPane().add(currentDisplay, BorderLayout.CENTER);
		frame.validate();	
	}

	public AudioInterface getAudioInterface() {
		return this.audioInterface;
	}

	@Override
	public synchronized void updateData(double[] newValues) {
		this.getAudioInterface().updateData(newValues);
		this.getChart().updateData(newValues);
		
	}
	
	@Override
	public synchronized void updateData(Complex[] newValues) {
		this.getAudioInterface().updateData(newValues);
		this.getChart().updateData(newValues);
		
	}

	@Override
	public void connectToData(DataStreamInformation streamInformation) {
		this.streamInformation = streamInformation;
		audioInterface = new AudioInterface(10*((Double)streamInformation.getAttribute(DataStreamInformation.SAMPLERATE)).intValue()); //10 seconds of save time
		this.getAudioInterface().connectToData( streamInformation);
		this.getChart().connectToData( streamInformation);
		
	}

	@Override
	public void disconnectFromData() {
		this.getAudioInterface().disconnectFromData();
		this.getChart().disconnectFromData();
		
	}
}
