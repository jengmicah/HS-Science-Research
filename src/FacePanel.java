import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import org.opencv.core.Point;


public class FacePanel extends JPanel {
	private int width;
	private int height;
	List<Point> points = null;

	public FacePanel(int width, int height){  
		super();  
		this.width = width;
		this.height = height;
	} 

	public void addPoints(List<Point> points) {
		this.points = points;
	}

	protected void paintComponent(Graphics g){  
		super.paintComponent(g);  
		Graphics2D g2 = (Graphics2D)g;

		g2.setColor(Color.blue);
		if(!points.isEmpty() && points != null) {
			for(int i = 0; i < points.size(); i++) {
				g2.fillOval((int)points.get(i).x, (int)points.get(i).y, 7, 7);
			}
		}
	}
}
