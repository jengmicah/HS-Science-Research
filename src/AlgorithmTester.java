import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

/*
 * The idea for the algorithm tester is to use the Viola-Jones algorithm, which works best 
 * for upright faces, to look for faces in the rotated images in order to see if the faces
 * in the rotated images are upright, which is the goal of the proposed algorithm. 
 */
public class AlgorithmTester {
	static String faceCascade = "C:/opencv/haarcascades/haarcascade_frontalface_default.xml";
	static CascadeClassifier faceDetector;
	static MatOfRect faceArray;
	///////////////
	static String fileName = "_SR Trial 17";
	static int h = 1;
	static int numOfPics = 223;
    ///////////////
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		faceDetector = new CascadeClassifier(faceCascade);
		faceArray = new MatOfRect();

		Mat image = new Mat();

		for(int i = 1; i <= numOfPics; i++) {
			image = Imgcodecs.imread("C:/" + fileName + "/Rotated/rotated" + h + ".png");
			if(image != null) {
				if(!image.empty()) {
					detectFace(image);
					System.out.println(i + "/" + numOfPics);
				}
			}
			h++;
		}
	}

	public static void detectFace(Mat image) {
		Mat imageGray = new Mat();
		if(image.channels() != 1)
			Imgproc.cvtColor(image, imageGray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(imageGray, imageGray);  //equalize the image
		faceDetector.detectMultiScale(imageGray, faceArray, 1.3, 5, Objdetect.CASCADE_SCALE_IMAGE | Objdetect.CASCADE_DO_CANNY_PRUNING | Objdetect.CASCADE_DO_ROUGH_SEARCH, new Size(image.cols()/10, image.rows()/10), new Size(image.cols(), image.rows()));
		for (Rect rect : faceArray.toArray()) {//go through the faces vector and draw rectangles around any faces
			Imgproc.rectangle(imageGray, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 0), 3); //draws the rectangle on the original image (compensates for the x and y of ROI being (0,0))
			Imgcodecs.imwrite("C:/" + fileName + "/Tested/rotated" + h + ".png", imageGray);
		}
	}
}
