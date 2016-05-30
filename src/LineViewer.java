import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.opencv.core.Point;


public class LineViewer extends JFrame {
	
	JFrame frame;
	LinePanel panel; 
	boolean positive;
	
	public LineViewer(int videoWidth, int videoHeight) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double screenWidth = screenSize.getWidth();

		int frameWidth = videoWidth + 35;
		int frameHeight = videoHeight + 60;
		
		frame = new JFrame("Angle of Inclination");  
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		frame.setBounds((int)screenWidth/2,0, frameWidth, frameHeight); 
		panel = new LinePanel(frameWidth, frameHeight);   
		frame.setContentPane(panel); 
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Exiting..");
				e.getWindow().dispose();
			}
		});
	}
	
	public void addPoints(Point pt1, Point pt2) {
		panel.addPoints(pt1,pt2, findAngle(pt1, pt2));
		panel.repaint();
	}
	
	public double findAngle(Point pt1, Point pt2) {
		//rad((x2 - x1)sq + (y2 - y1)sq)
		double opposite = Math.sqrt(Math.pow(pt2.y - pt1.y, 2)); //length of opposite side
		double hypotenuse = Math.sqrt(Math.pow(pt1.x - pt2.x,2) + Math.pow(pt2.y - pt1.y, 2)); //length of hypotenuse
		double angle = Math.toDegrees(Math.asin(opposite/hypotenuse)); //arcsine function
		return angle; //get angle
	}
	
	public boolean checkPositivity(Point pt1, Point pt2) {
		if(pt2.y > pt1.y) {
			positive = true; //opposite
		} else {
			positive = false; //normal
		}
		return positive;
	}
}
