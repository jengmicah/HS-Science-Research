import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class RotationMat {

	boolean positive; 
	
	public RotationMat(boolean positive) {
		this.positive = positive;
	}
	
	public int getDiagonal(Mat src) {
		return (int)Math.sqrt(Math.pow(src.cols(), 2) + Math.pow(src.rows(), 2)); //a^2 + b^2 = c^2
	}
	
	public void rotate(Mat src, Mat dst, double angle, int diag) {
		if(src != null) {
			if(!src.empty() && src.rows() > 0 && src.cols() > 0) {
//				int srcCenterX = src.cols()/2;
//				int srcCenterY = src.rows()/2;
//				int dstCenterX = diag/2;
//				int dstCenterY = diag/2;
//				int deltaX = dstCenterX - srcCenterX;
//				int deltaY = dstCenterY - srcCenterY;
				
				Point pt = new Point(src.cols()/2, src.rows()/2);
				Mat M = new Mat();
				if(positive) M = Imgproc.getRotationMatrix2D(pt, angle, 1.0);
				else M = Imgproc.getRotationMatrix2D(pt, -angle, 1.0);
				Imgproc.warpAffine(src, dst, M, new Size(diag,diag));
			}
		}
	}
}
