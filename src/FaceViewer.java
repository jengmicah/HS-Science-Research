import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JFrame;

import org.opencv.core.Point;


public class FaceViewer {
	
	JFrame frame;
	FacePanel panel; 
	
	public FaceViewer(int videoWidth, int videoHeight) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double screenWidth = screenSize.getWidth();
		double screenHeight = screenSize.getHeight();

		int frameWidth = videoWidth + 35;
		int frameHeight = videoHeight + 60;
		
		frame = new JFrame("Face Points");  
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		frame.setBounds((int)screenWidth/2,(int)screenHeight/2, frameWidth, frameHeight); 
		panel = new FacePanel(frameWidth, frameHeight);   
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
	
	public void addPoints(List<Point> points) {
		panel.addPoints(points);
		panel.repaint();
	}
}
