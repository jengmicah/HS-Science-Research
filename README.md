# Science-Research
Real-time rotation invariant face tracking using an enhanced Viola-Jones detection algorithm

Current methods of face tracking often fail to detect rotated faces and cannot detect faces in complex backgrounds. My research proposes a method of rotation-invariant and background-resistant face tracking that requires a minimum amount of information, which will in turn maximize efficiency while maintaining accuracy. The proposed algorithm utilizes the Viola-Jones algorithm for the initial face image, applies a corner detector, and implements an optical flow algorithm to track the face and corrects any changes in head orientation. A feedback system is implemented to correct any points that stray away from the concentration of points around the face. The angle of inclination from the line formed between the nostrils is used to determine the degree of rotation of the head. This algorithm yields a 96.72% detection rate under a solid-colored background and an 82.81% detection rate under a complex background, compared to the detection rate of 19.28% of rotated faces utilizing the isolated Viola-Jones algorithm.

Updated for OpenCV 3.1.0.
