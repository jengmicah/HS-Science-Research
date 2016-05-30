/*
 * 4/27: instead of drawing box around all points, have a set ratio between length and width
 * 			-draw box around (meanX, meanY)
 * 			-will have to check for distance from camera (whether or not to resize the box, but keep length:width ratio)
 * have implemented optical flow (4/6) - initializes with certain number of points and when face appears
 * created the face tracker (4/22) - need to enhance the function involving the distance from the centroid that will accept points
 * 			-think about implementing gaze tracking/3d head tracking
 */
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


public class ScienceResearchMain {
	static Rect objectBoundingRectangle = new Rect(0,0,0,0);
	int camNum = 0;
	int numWebcams;
	private JFrame liveFrame, faceFrame, rotateFrame, featureFrame;
	//output images are displayed here
	private Panel livePanel, facePanel, rotatePanel, featurePanel;
	private JButton camButton;
	private boolean update = true;
	private VideoCapture webcam;
	int videoHeight, videoWidth;
	private final int SENSITIVITY_VALUE = 0;

	//images we work with
	Mat currentColor, nextColor, currentGray1, nextGray1, currentGray2, nextGray2;

	//Shi and Tomasi Feature Tracking Attributes
	MatOfPoint corners;
	int maxCorners = 400;
	double qualityLevel = 0.01;
	double minDistance = 10;
	int blockSize = 3;
	boolean useHarrisDetector = false;
	double k = 0.04;
	boolean needToInit = true;
	boolean needToInit2 = true;

	//Pyramidal Lucas Kanade Optical Flow
	MatOfPoint2f prevPts;
	MatOfPoint2f nextPts;
	MatOfByte status;
	MatOfFloat err;
	MatOfByte status2;
	MatOfFloat err2;
	Size winSize; 
	Size winSize2;
	int maxlvl = 5;
	TermCriteria termcrit;
	boolean tracking = false;

	List<Point> faceCornersPrev, faceCornersNext, featureCornersPrev, featureCornersNext;
	List<Byte> faceByteStatus, featureByteStatus;
	//used to compute motion vector
	static final double pi = 3.14159265358979323846;

	//face detection 
	MatOfRect faceArray, noseArray;
	String faceCascade = "C:/lib/opencv/sources/data/haarcascades/haarcascade_frontalface_default.xml";
	String noseCascade = "C:/lib/opencv/sources/data/nose.xml";
	CascadeClassifier faceDetector, noseDetector;
	int rectX, rectY;
	Mat faceImage, noseImage;
	List<Point> cornersNextLinked;
	int numOfInitialFeatures;
	boolean alreadyExec = false;

	int numOfFrames = 0;

	//fps calculations
	long start, end;
	double fps;	// fps calculated using number of frames / seconds
	int counter = 0; // frame counter
	double sec;	// floating point seconds elapsed since start

	//centers
	double meanX, meanY;

	Rect opticalRect;
	int noseX, noseY;

	//features
	Point[] features;
	Mat opticalFace, opticalNose;
	MatOfPoint2f features2;
	MatOfPoint2f features1;
	boolean alreadyExec2 = false;

	LineViewer lineviewer;
	FaceViewer faceviewer;
	Mat rotatedMat;
	boolean positive = false;
	int cropValue = 17;//removes side noise
	int topCropValue = 45; //a lot of noise (cuts it down so that it definitely detects the nostrils)
	double distance1 = 0, distance2 = 0;
	RotationMat mat;
	double noseDistance = 15;

	int totalFrames = 0;
	//	int imageNum = 0;
	//	boolean startSave = false;
	//	String fileName = "_SR Trial 17"; //<<<< CHANGE THIS <<<<<<

	public void initialize() {
		//initialize all objects
		currentColor = new Mat(); nextColor = new Mat(); 
		currentGray1 = new Mat(); nextGray1 = new Mat(); currentGray2 = new Mat(); nextGray2 = new Mat();
		opticalFace = new Mat(); opticalNose = new Mat();
		//Shi and Tomasi Feature Tracking Attributes
		corners = new MatOfPoint();

		//Pyramidal Lucas Kanade Optical Flow
		prevPts = new MatOfPoint2f(); nextPts = new MatOfPoint2f(); 
		status = new MatOfByte();
		err = new MatOfFloat();
		status2 = new MatOfByte();
		err2 = new MatOfFloat();
		winSize = new Size(20,20);
		winSize2 = new Size(20,20);
		termcrit = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS,20,0.03);

		//Face Detection
		faceArray = new MatOfRect(); noseArray = new MatOfRect();
		faceDetector = new CascadeClassifier(faceCascade);
		noseDetector = new CascadeClassifier(noseCascade);
		faceImage = new Mat(); noseImage = new Mat();

		features = new Point[2];
		for(int i = 0; i < features.length; i++) {
			features[i] = new Point();
		}
		features1 = new MatOfPoint2f();
		features2 = new MatOfPoint2f();
	}

	public ScienceResearchMain() {
		//load native library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//receive input from user for which webcam to use
		//getCamInput();
		//load webcam
		loadWebcam();
		//load GUI(s)
		setFrame();
		//initialize all objects
		initialize();
		//displays bounding box of foreground in the end
		update();
	}

	public void update() {
		webcam.read(currentColor); //only need to read "current" frame once (after it runs, the current frame becomes the next frame)
		if(!currentColor.empty()) { //if the webcam captures a frame, 
			Imgproc.cvtColor(currentColor, currentGray1, Imgproc.COLOR_BGR2GRAY); //turn to grayscale
			currentGray2 = currentGray1.clone();
		} else System.exit(1); //exit with an error if there is no frame found

		while(update) {
			webcam.read(nextColor); //get the second frame of video
			if(!nextColor.empty()) { //same principles as the first
				Imgproc.cvtColor(nextColor, nextGray1, Imgproc.COLOR_BGR2GRAY);
				nextGray2 = nextGray1.clone();
			} else System.exit(1);

			//face detection/tracking
			if(needToInit) {
				faceImage = detectFace(nextGray1, nextColor); 
				if(!faceArray.empty()) {
					getCorners(faceImage, corners, maxCorners, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, k);
					alreadyExec = false;
				}
			} else {  
				faceOpticalFlow(currentGray1, nextGray1, prevPts, nextPts, status, err, winSize, maxlvl);
			}
			if(needToInit2) {
				noseImage = detectNose(nextGray1, nextColor);
				if(!noseArray.empty() && !noseImage.empty() && noseArray != null && noseImage != null) {
					getFeaturePoints(noseImage, nextColor);
				}
			}
			else {
				featureOpticalFlow(currentGray2, nextGray2, features1, features2, status2, err2, winSize2, maxlvl);
			}
			//faceImage = detectFace(nextGray, nextColor); 
			//noseImage = detectNose(nextGray, nextColor);
			++counter;
			if(counter >= 15) {
				faceImage = detectFace(nextGray1, nextColor); 
				if(!faceArray.empty()) {
					getCorners(faceImage, corners, maxCorners, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, k); //gather new points and it will be passed in the previous opticalFlow() method
					alreadyExec = false;
				}
				noseImage = detectNose(nextGray1, nextColor);
				if(!noseArray.empty()) {
					getFeaturePoints(noseImage, nextColor);
					alreadyExec2 = false;
				}
				counter = 0;
			}

			totalFrames++;
			//			Core.putText(nextColor, String.valueOf(totalFrames), new Point(10,40), 2, 1.0, new Scalar(255,255,0), 2, 1, false);

			////////////DISPLAY IMAGE///////////
			drawOnFrame(opticalFace, noseImage, rotatedMat, nextColor);
		}
		//		BufferedWriter writer;
		//		try {
		//			writer = new BufferedWriter(new FileWriter(new File("C:/" + fileName + "/Total Frames.txt")));
		//			writer.write("Total Frames: " + totalFrames);
		//			writer.close();
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		System.out.println("Total Frames: " + totalFrames);
	}

	//////////////////////// FACE /////////////////////////////
	public void faceOpticalFlow(Mat image1, Mat image2, MatOfPoint2f prevPts, MatOfPoint2f nextPts, MatOfByte status, MatOfFloat err, Size winSize, int maxlvl) {
		//Pyramidal Lucas Kanade Optical Flow
		Video.calcOpticalFlowPyrLK(image1, image2, prevPts, nextPts, status, err, winSize, maxlvl);

		faceCornersPrev = prevPts.toList(); //convert to Lists for easier access
		faceCornersNext = nextPts.toList();
		faceByteStatus = status.toList();

		for(int i = 0; i < prevPts.rows(); i++) { //cycle through points and perform operations
			if(faceByteStatus.get(i) == 1) { //if there is a feature, do the following
				//***//
				if(i < faceCornersNext.size()) {
					//					Core.circle(nextColor, new Point(faceCornersNext.get(i).x, faceCornersNext.get(i).y), 1, new Scalar(0,0,255),5);
					findCenter(nextColor, faceCornersNext);
					faceCornersNext = checkPoints(nextColor, faceCornersNext, faceCornersPrev);
					prevPts.fromList(faceCornersNext); //current points become next points (continues to track)
					drawBox(nextColor, faceCornersNext, prevPts);
					faceviewer.addPoints(faceCornersNext);
				}
			}
		}
		currentGray1 = nextGray1.clone(); //current frame becomes next frame (the webcam will continue to read the next frame)
	}

	public Mat detectFace(Mat imageGray, Mat imageColor) {
		Mat faceTemp = new Mat();
		Imgproc.equalizeHist(imageGray, imageGray);  //equalize the image
		faceDetector.detectMultiScale(imageGray, faceArray, 1.2, 5, Objdetect.CASCADE_SCALE_IMAGE | Objdetect.CASCADE_DO_CANNY_PRUNING | Objdetect.CASCADE_DO_ROUGH_SEARCH, new Size(videoWidth/10, videoHeight/10), new Size(videoWidth, videoHeight));
		for (Rect rect : faceArray.toArray()) {//go through the faces vector and draw rectangles around any faces
			//			Core.rectangle(imageColor, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 0), 3); //draws the rectangle on the original image (compensates for the x and y of ROI being (0,0))
			faceTemp = imageColor.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width); //creates a matrix for the face
			rectX = rect.x; rectY = rect.y;
		}
		if(faceTemp.channels() != 1)
			Imgproc.cvtColor(faceTemp, faceTemp, Imgproc.COLOR_BGR2GRAY); //turn face image to grayscale if not grayscale already
		return faceTemp; //assuming only one person in view (must change for multiple ppl)
	}

	public void findCenter(Mat image, List<Point> points) {
		meanX = 0;
		meanY = 0;

		for(int i = 0; i < points.size(); i++) { //cycle through points and perform operations
			meanX += points.get(i).x;
			meanY += points.get(i).y;
		}
		meanX /= points.size();
		meanY /= points.size();
		//		Core.circle(image, new Point(meanX, meanY), 1, new Scalar(0,255,0),9);
	}

	public List<Point> checkPoints(Mat image, List<Point> cornersNext, List<Point> cornersPrev) {
		double distanceX, distanceY;
		if(camNum == 0) {
			distanceX = 120;
			distanceY = 120;
		} else {
			distanceX = 210;
			distanceY = 210;
		}

		cornersNextLinked = new LinkedList<Point>(cornersNext); //cornersNext is converted to LinkedList in order to remove the elements
		if(!alreadyExec) {
			numOfInitialFeatures = cornersNextLinked.size();
			alreadyExec = true;
		}

		for (Iterator<Point> iter = cornersNextLinked.listIterator(); iter.hasNext(); ) { //remove all points that travel a certain number of pixels away from the center (avgX, avgY)
			Point a = iter.next();
			if (a.x > meanX + distanceX || a.x < meanX - distanceX || a.y > meanY + distanceY || a.y < meanY - distanceY || a.x < 0 || a.x > liveFrame.getWidth() || a.y < 0 || a.y > liveFrame.getHeight())
				iter.remove();
		}
		//		System.out.println("INITIAL: " + numOfInitialFeatures);
		//		System.out.println(cornersNextLinked.size());

		if(cornersNextLinked.size() <= numOfInitialFeatures/2) { //if the # of points goes below half the initial number of points, then restart
			needToInit = true;
		}
		return cornersNextLinked;
	}

	public void drawBox(Mat image, List<Point> points, MatOfPoint2f prevPts) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for(int i = 0; i < points.size(); i++) { //cycle through points and perform operations
			if(points.get(i).x < minX)
				minX = (int)points.get(i).x;
			if(points.get(i).y < minY)
				minY = (int)points.get(i).y;
			if(points.get(i).x > maxX)
				maxX = (int)points.get(i).x;
			if(points.get(i).y > maxY)
				maxY = (int)points.get(i).y;
		}

		int length = (int)(maxY - minY);
		int width = (int)(maxX - minX); 
		if(length > width) opticalRect = new Rect(minX, minY, length, length);
		else opticalRect = new Rect(minX, minY, width, width);
		//		System.out.println("MinX: " + minX + ", MinY: " + minY);
		//		Imgproc.putText(nextColor, Integer.toString(width), new Point(rectX  + width/2-15, rectY-10), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,0,0));
		//		Imgproc.putText(nextColor, Integer.toString(length), new Point(rectX + width+5, rectY + length/2), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,0,0));
		if(0 <= opticalRect.x && 0 <= opticalRect.width && opticalRect.x + opticalRect.width <= nextColor.cols() && 0 <= opticalRect.y && 0 <= opticalRect.height && opticalRect.y + opticalRect.height <= nextColor.rows()) {
			opticalFace = nextColor.submat(opticalRect);
			rectX = opticalRect.x; rectY = opticalRect.y;
			//			Imgproc.circle(nextColor, new Point(minX, minY), 1, new Scalar(255,0,0),5);
			//			Imgproc.circle(nextColor, new Point(maxX, maxY), 1, new Scalar(255,0,0),5);
			//			Imgproc.circle(nextColor, new Point(opticalRect.width/2 + opticalRect.x, opticalRect.height/2 + opticalRect.y), 1, new Scalar(255,0,255),20);
			Imgproc.rectangle(nextColor, new Point(opticalRect.x,opticalRect.y), new Point(opticalRect.x + opticalRect.width, opticalRect.y + opticalRect.height), new Scalar(255,0,255),5); //final bounding box
		}
	}

	public void getCorners(Mat image, MatOfPoint corners, int maxCorners, double qualityLevel, double minDistance, Mat mask, int blockSize, boolean useHarrisDetector, double k) {
		//Shi and Tomasi Feature Tracking
		Imgproc.goodFeaturesToTrack(image, corners, maxCorners, qualityLevel, minDistance, mask, blockSize, useHarrisDetector, k);
		MatOfPoint2f corners2f = new MatOfPoint2f(corners.toArray());
		Imgproc.cornerSubPix(image, corners2f, new Size(5, 5), new Size(-1,-1), new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 40, 0.001 ));
		//		tracking = true;
		prevPts.fromArray(corners2f.toArray()); //convert output corners (MatOfPoint) to MatOfPoint2f (input for calcOpticalFlowPyrLK)
		List<Point> cornersPrevTemp = prevPts.toList();

		for (int i = 0; i < prevPts.rows(); i++) { //account for the x and y coordinates of face and original image
			cornersPrevTemp.get(i).x += rectX;
			cornersPrevTemp.get(i).y += rectY;
		}
		prevPts.fromList(cornersPrevTemp);
		if(cornersPrevTemp.size() > 20)
			needToInit = false; //if there are points, then there's no need to keep finding the features again (we'll be tracking them)
	}


	///////////////////////// FEATURE ///////////////////////////
	public void featureOpticalFlow(Mat image1, Mat image2, MatOfPoint2f features1, MatOfPoint2f features2, MatOfByte status, MatOfFloat err, Size winSize, int maxlvl) {
		//Pyramidal Lucas Kanade Optical Flow
		Video.calcOpticalFlowPyrLK(image1, image2, features1, features2, status, err, winSize, maxlvl);

		featureCornersPrev = features1.toList(); //convert to Lists for easier access
		featureCornersNext = features2.toList();
		featureByteStatus = status.toList();

		for(int i = 0; i < features1.rows(); i++) { //cycle through points and perform operations
			if(featureByteStatus.get(i) == 1) { //if there is a feature, do the following
				//***//
				if(i < featureCornersNext.size()) {
					//										Core.circle(nextColor, new Point(featureCornersNext.get(i).x, featureCornersNext.get(i).y), 1, new Scalar(0,0,0),5);
					//										drawFeatureLine(featureCornersNext.get(0),featureCornersNext.get(1));
					checkFeaturePoint(featureCornersNext.get(0),featureCornersNext.get(1));
					features1.fromList(featureCornersNext); //current points become next points (continues to track)
					lineviewer.addPoints(featureCornersNext.get(0),featureCornersNext.get(1));
					positive = lineviewer.checkPositivity(featureCornersNext.get(0),featureCornersNext.get(1));

					mat = new RotationMat(positive);
					int diagonal = mat.getDiagonal(opticalFace);
					rotatedMat = new Mat(diagonal,diagonal,opticalFace.type());
					mat.rotate(opticalFace,rotatedMat,lineviewer.findAngle(featureCornersNext.get(0),featureCornersNext.get(1)),diagonal);
				}
			}
		}
		currentGray2 = nextGray2.clone(); //current frame becomes next frame (the webcam will continue to read the next frame)
	}

	public void drawFeatureLine(Point pt1, Point pt2) {
		Imgproc.line(nextColor, pt1, pt2, new Scalar(0,255,255), 2);
	}

	public Mat detectNose(Mat imageGray, Mat imageColor) {
		Mat noseTemp = new Mat();
		Imgproc.equalizeHist(imageGray, imageGray);  //equalize the image
		if(imageGray.channels() != 1)
			Imgproc.cvtColor(imageGray, imageGray, Imgproc.COLOR_BGR2GRAY); //turn face image to grayscale if not grayscale already
		//		if(imageGray.empty()) {System.out.println("EMPTY");System.exit(0);;}
		noseDetector.detectMultiScale(imageGray, noseArray, 1.4, 5, Objdetect.CASCADE_SCALE_IMAGE | Objdetect.CASCADE_DO_CANNY_PRUNING | Objdetect.CASCADE_DO_ROUGH_SEARCH, new Size(videoWidth/10, videoHeight/10), new Size(videoWidth, videoHeight));
		for (Rect rect : noseArray.toArray()) {//go through the faces vector and draw rectangles around any faces
			//			Core.rectangle(imageColor, new Point(rect.x + opticalRect.x, rect.y + opticalRect.y), new Point(rect.x + opticalRect.x + rect.width, rect.y + opticalRect.y + rect.height), new Scalar(0, 0, 0), 3); //draws the rectangle on the original image (compensates for the x and y of ROI being (0,0))
			//			noseTemp = imageColor.submat(rect.y+ opticalRect.y, rect.y+ opticalRect.y + rect.height, rect.x+ opticalRect.x, rect.x + opticalRect.x+ rect.width); //creates a matrix for the face
			//						Core.rectangle(imageColor, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 0), 3); //draws the rectangle on the original image (compensates for the x and y of ROI being (0,0))
			noseTemp = imageColor.submat(rect.y, rect.y+ rect.height, rect.x, rect.x + rect.width); //creates a matrix for the face
			noseX = rect.x; noseY = rect.y;
		}
		if(noseTemp.channels() != 1)
			Imgproc.cvtColor(noseTemp, noseTemp, Imgproc.COLOR_BGR2GRAY); //turn face image to grayscale if not grayscale already
		Imgproc.threshold(noseTemp, noseTemp, SENSITIVITY_VALUE, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU); //then to binary image 
		Imgproc.morphologyEx(noseTemp, noseTemp, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5), new Point(3,3))); //apply basic morphological operations

		Mat croppedNose = new Mat();
		if(!noseTemp.empty() && noseTemp != null) {
			if(noseTemp.rows() > topCropValue+cropValue && noseTemp.cols() > cropValue*2)
				croppedNose = noseTemp.submat(topCropValue, noseTemp.rows()-cropValue, cropValue, noseTemp.cols()-cropValue); //removes any white corner pixels that hinder detection (takes off 5 pixels from each side)
		}
		return croppedNose; //assuming only one person in view (must change for multiple ppl)
	}

	public void checkFeaturePoint(Point pt1, Point pt2) {
		if(!alreadyExec2) {
			distance1 = Math.sqrt(Math.pow(pt2.x-pt1.x,2) + Math.pow(pt2.y-pt1.y, 2)); //distance formula
			alreadyExec2 = true;
		}
		distance2 = Math.sqrt(Math.pow(pt2.x-pt1.x,2) + Math.pow(pt2.y-pt1.y, 2));
		//		System.out.println("Distance1: " + distance1);
		//		System.out.println("Distance2: " + distance2);
		if(distance2 >= distance2 + noseDistance || distance2 <= distance2 - noseDistance || pt1.x <= 0 || pt1.y <= 0 || pt2.x <= 0 || pt2.y <= 0 || pt1.x >= liveFrame.getWidth() || pt1.y >= liveFrame.getHeight() || pt2.x >= liveFrame.getWidth() || pt2.y >= liveFrame.getHeight()) {
			needToInit2 = true;
		}
	}

	public void getFeaturePoints(Mat noseImage, Mat nextColor) {	
		boolean contourDetected = false;
		Mat temp = new Mat();
		temp = noseImage.clone();
		//these two vectors needed for output of findContours
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		MatOfInt4 hierarchy = new MatOfInt4();
		//find contours of filtered image using openCV findContours function
		//Imgproc.findContours(temp,contours,hierarchy,Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_SIMPLE );// retrieves all contours
		if(temp!= null && !temp.empty())
			Imgproc.findContours(temp,contours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE );// retrieves external contours
		//		Imgproc.drawContours(nextColor, contours, -1, new Scalar(0,255,0), 2);
		//if contours vector is not empty, we have found some objects
		if(contours.size() > 0) {contourDetected = true;}
		else {contourDetected = false;}
		if(contourDetected){
			Point leftCorner = new Point(15 + noseX + cropValue, noseImage.rows()/2 + noseY + topCropValue);
			Point rightCorner = new Point(noseImage.cols()-10 + noseX + cropValue, noseImage.rows()/2 + noseY + topCropValue);
			features[0] = leftCorner; //these points are stored in the features object and then tracked
			features[1] = rightCorner;
			features1.fromArray(features); //create the prevPts of the features optical flow (converts array to MatOfPoint2f)
			needToInit2 = false; //now it starts tracking
		}
	}

	//////////////////////////// PANELS ////////////////////////////
	public void drawOnFrame(Mat image1, Mat image2, Mat image3, Mat image4) {
		//		imageNum++;

		if(image4 != null) {
			if(!image4.empty()) {
				livePanel.setImageWithMat(image4);
				livePanel.repaint();
				//				if(startSave)
				//					Highgui.imwrite("C:/" + fileName + "/Upright/upright" + imageNum + ".png", image4);
			}
		}
		if(image1 != null) {
			if(!image1.empty()) {
				facePanel.setImageWithMat(image1);
				facePanel.repaint();
			}
		}
		if(image2 != null) {
			if(!image2.empty()) {
				featurePanel.setImageWithMat(image2);
				featurePanel.repaint();
			}
		}
		if(image3 != null) {
			if(!image3.empty()) {
				rotatePanel.setImageWithMat(image3);
				rotatePanel.repaint();
				//				if(startSave)
				//					Highgui.imwrite("C:/" + fileName + "/Rotated/rotated" + imageNum + ".png", image3);
			}
		}
	}

	public void setFrame() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double screenWidth = screenSize.getWidth();
		double screenHeight = screenSize.getHeight();

		int frameWidth = videoWidth + 35;
		int frameHeight = videoHeight + 60;

		liveFrame = new JFrame("Live Feed");  
		liveFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		liveFrame.setBounds(0,0, frameWidth, frameHeight); 
		camButton = new JButton("Start Test");
		camButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				totalFrames = 0;
				//				imageNum = 0;
				//				startSave = true;
			}
		});  
		livePanel = new Panel(frameWidth, frameHeight); 
		livePanel.add(camButton);
		liveFrame.setContentPane(livePanel); 
		liveFrame.setVisible(true);  
		liveFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Exiting..");
				update = false;
				webcam.release();
				e.getWindow().dispose();
			}
		});

		featureFrame = new JFrame("Nose");  
		featureFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		featureFrame.setBounds((int)(screenWidth/2) - 250, (int)(screenHeight * 3 / 4), 200,200);  
		featurePanel = new Panel(190,140);  
		featureFrame.setContentPane(featurePanel); 
		featureFrame.setVisible(true);
		featureFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Exiting..");
				update = false;
				webcam.release();
				e.getWindow().dispose();
			}
		});

		lineviewer = new LineViewer(videoWidth, videoHeight);
		faceviewer = new FaceViewer(videoWidth, videoHeight);

		faceFrame = new JFrame("Face");  
		faceFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		faceFrame.setBounds((int)(screenWidth/2) - 150,0, 350,350);  
		facePanel = new Panel(350,350);  
		faceFrame.setContentPane(facePanel); 
		faceFrame.setVisible(true);  
		faceFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Exiting..");
				update = false;
				webcam.release();
				e.getWindow().dispose();
			}
		});
		rotateFrame = new JFrame("Rotated Face");  
		rotateFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		rotateFrame.setBounds((int)screenWidth/2,0, frameWidth, frameHeight);  
		rotatePanel = new Panel(350,350);  
		rotateFrame.setContentPane(rotatePanel); 
		rotateFrame.setVisible(true);  
		rotateFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Exiting..");
				update = false;
				webcam.release();
				e.getWindow().dispose();
			}
		});
	}


	/////////////////////////// WEBCAM ////////////////////////////////
	public void loadWebcam() {
		webcam = new VideoCapture();
		webcam.open(camNum);
		webcam.set(Videoio.CAP_PROP_FRAME_WIDTH, 720); //set resolution of webcam
		webcam.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 480);
		videoWidth = (int)webcam.get(Videoio.CV_CAP_PROP_FRAME_WIDTH);
		videoHeight = (int)webcam.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT);
		if(webcam.isOpened()) { System.out.println("Found webcam: " + webcam.toString()); } //check if webcam has been turned on
		else { JOptionPane.showMessageDialog(null,"Webcam was not detected\nShutting down..."); System.exit(0); }
	}

	public int countWebcams(int numWebcams) {
		VideoCapture tempWebcam;
		numWebcams = 0;
		boolean opened = true;
		while(opened) {
			tempWebcam = new VideoCapture(numWebcams);
			if(tempWebcam.isOpened()) {
				numWebcams++;
				opened = true;
			} else {
				opened = false;
			}
			tempWebcam.release();
		}
		return numWebcams;
	}

	public void setCamNum(int camNum) {
		this.camNum = camNum;
	}

	public void getCamInput() {
		//get camera number from user
		int numOfCams = countWebcams(numWebcams);
		if(numOfCams == 1) {
			setCamNum(0);
		} else if(numOfCams == 0){
			JOptionPane.showMessageDialog(null,"Webcam was not detected\nShutting down..."); 
			System.exit(0);
		} else {
			String camNumInput = JOptionPane.showInputDialog("Number of Webcams: " + numOfCams + "\nEnter value of webcam (0 - " + (numOfCams - 1) + ")");
			setCamNum(Integer.parseInt(camNumInput));
		}
	}

	public static void main(String[]args) {
		new ScienceResearchMain();
	}
}

//***//
//				int line_thickness = 2;				
//				Scalar line_color = new Scalar(0,255,0);
//				Point p = new Point();
//				Point q = new Point();
//				p.x = (int) cornersPrev.get(i).x;
//				p.y = (int) cornersPrev.get(i).y;
//				q.x = (int) cornersNext.get(i).x;
//				q.y = (int) cornersNext.get(i).y;
//
//				double angle = Math.atan2((double) p.y - q.y, (double) p.x - q.x);
//				double hypotenuse = Math.sqrt((p.y - q.y)*(p.y - q.y) + (p.x - q.x)*(p.x - q.x));
//
//				/* lengthen the arrow by a factor of three. */
//				q.x = (int) (p.x - 3 * hypotenuse * Math.cos(angle));
//				q.y = (int) (p.y - 3 * hypotenuse * Math.sin(angle));
//
//				/* draw the main line of the arrow. */
//				/* "nextColor" is the frame to draw on.
//				 * "p" is the point where the line begins.
//				 * "q" is the point where the line stops.
//				 * "CV_AA" means antialiased drawing.
//				 * "0" means no fractional bits in the center cooridinate or radius.
//				 */
//				Core.line(nextColor, p, q, line_color, line_thickness, Core.LINE_AA, 0);
//				/* Now draw the tips of the arrow.  Some scaling is done so that the
//				 * tips look proportional to the main line of the arrow.
//				 */			
//				p.x = (int) (q.x + 9 * Math.cos(angle + pi / 4));
//				p.y = (int) (q.y + 9 * Math.sin(angle + pi / 4));
//				Core.line(nextColor, p, q, line_color, line_thickness, Core.LINE_AA, 0);
//				p.x = (int) (q.x + 9 * Math.cos(angle - pi / 4));
//				p.y = (int) (q.y + 9 * Math.sin(angle - pi / 4));
//				Core.line(nextColor, p, q, line_color, line_thickness, Core.LINE_AA, 0);