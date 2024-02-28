import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

public abstract class AbstractGraph extends Component implements IGraph{

	private static final long serialVersionUID = 4577545147680318083L;
	
	public abstract void saveCSV();
	
	public void saveImage() {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH mm ss");  
		   LocalDateTime now = LocalDateTime.now();  
		   Path target = FileSystems.getDefault().getPath(now.format(dtf)+".png");
		   this.saveImage(target);
	}
	public void saveImage(Path targetImage) {
		BufferedImage image=new BufferedImage(1024, 768,BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2=(Graphics2D)image.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setClip(0, 0, 1024, 768);
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, 1024, 768);
		g2.setColor(Color.BLACK);
		this.paint(g2);
		try {
			ImageIO.write(image, "png", targetImage.toFile());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public abstract void paint(Graphics g);

}
