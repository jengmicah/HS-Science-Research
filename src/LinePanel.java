import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

import javax.swing.JPanel;

import org.opencv.core.Point;


public class LinePanel extends JPanel {
	private int width;
	private int height;
	Point pt1 = null, pt2 = null;
	double angle;
	
	public LinePanel(int width, int height){  
		super();  
		this.width = width;
		this.height = height;
	} 
	
	public void addPoints(Point pt1, Point pt2, double angle) {
		this.pt1 = pt1;
		this.pt2 = pt2;
		this.angle = angle;
	}
	
	protected void paintComponent(Graphics g){  
		super.paintComponent(g);  
		Graphics2D g2 = (Graphics2D)g;
		
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(3));
        
	    if(pt1 != null && pt2 != null) {
	    	g2.drawString(String.valueOf(angle), width/2, 20);
	    	g2.setPaint(Color.black); //feature line
	    	g2.draw(new Line2D.Double(pt1.x, pt1.y, pt2.x, pt2.y));
	    	g2.setPaint(Color.red); //horizontal line
	    	g2.draw(new Line2D.Double(pt1.x, pt1.y, pt2.x, pt1.y));
	    	
//	    	g2.setPaint(Color.black); //feature line
//	    	g2.draw(new Line2D.Double(width/2, height/2, width/2 + 50* Math.sin(angle), height/2 + 50*Math.cos(angle)));
//	    	g2.setPaint(Color.red);//horizontal line
//	    	g2.draw(new Line2D.Double(width/2, height/2, width-10, height/2));
	    }
	}
}