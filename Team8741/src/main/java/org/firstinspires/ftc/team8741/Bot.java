/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.team8741;

import android.graphics.Color;
import android.media.MediaPlayer;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.List;

public class Bot {

    final static int ENCODER_TICKS_PER_REV = 1120;
    final static int WHEEL_DIAMETER = 6; //Inches

    final static double INCHES_PER_TICK = (WHEEL_DIAMETER * Math.PI) / ENCODER_TICKS_PER_REV;
    final static int DRIVE_THRESHOLD = (int) (0.5 / INCHES_PER_TICK);

    private final static double HEADING_THRESHOLD = 2.1;

    private final double goldThreshold = 600;

    int _leftOffset;
    int _rightOffset;

    private static final String TFOD_MODEL_ASSET = "RoverRuckus.tflite";
    private static final String LABEL_GOLD_MINERAL = "Gold Mineral";
    private static final String LABEL_SILVER_MINERAL = "Silver Mineral";

    private static final String VUFORIA_KEY = "AayLJf7/////AAABmVuaR0sGrEl/q1NavYUhLWpSlPllp9Bbe3BrknPrE8vykZ19I74yB2sxMw40YrDsQeANKcYA5fhhSqzbpzt9EtxutVSphCi5ADEbporcMa6Vx4rsq3RjWbC8o9uvpL6h8E6/uwRsu4Lu/AgegnCb33iVY52kwg7TpdacFWj7tGP1gCw4+FInFHiU9WGoW0CKeGIsOUZ6FqZFM0MST0jlz7rnu2kE7wrrd+GhpOCHK+jv1MvQzfT93jsq9xDfon+yLsAEKnLSc/mrzuile4twM3qTJAaeOHMByiB5n/awQ0POLT5+YpyWWsvE8TemZ8RSQlRHeMKdEj2BNeGb7aZZvgvFUD4HkBeywiKXoX0IzY4H\n";
    private final static double P_TURN_COEFF = 0.050;   // Larger is more responsive, but also less stable
    private final static double P_DRIVE_COEFF = 0.00060 ;  // Larger is more responsive, but also less stable
    private final static double F_MOTOR_COEFF = 0.09;   //Minimum amount of power given to motor from control loop
    private final static double HOLD_TIME = 0.7; //number of milliseconds the bot has to hold a position before the turn is completed

    private final static double AUTO_DRIVE_SPEED = 0.6;
    private final static double AUTO_TURN_SPEED = 0.7;
    private final static double POWER_DAMPEN = .001;
    private final static double TIMEOUT = 5000;

    private final static int YELLOW_THRESHOLD = 10;
    private final static int YELLOW_LIMIT = 10;

    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private DcMotor liftArm = null;
    private ColorSensor colorSensor = null;
    private Servo servo = null;

    private LinearOpMode opMode = null;

    private HardwareMap hwMap = null;

    private BNO055IMU imu = null;
    private Orientation angles = null;
    private Acceleration gravity = null;

    private TFObjectDetector tfod;
    private VuforiaLocalizer vuforia;

    private ElapsedTime time = new ElapsedTime();
    private boolean timerStarted = false;

    public Bot(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init(HardwareMap ahwMap) {


        hwMap = ahwMap;

        //Instantiate motor objects
        leftDrive = hwMap.get(DcMotor.class, "left");
        rightDrive = hwMap.get(DcMotor.class, "right");
        liftArm = hwMap.get(DcMotor.class, "lift");
        //colorSensor = hwMap.get (ColorSensor.class, "sensor_color");
        servo = hwMap.get (Servo.class, "servo");


        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = false;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu = hwMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        setRightDirection(DcMotor.Direction.REVERSE);
        setBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        liftArm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        //leftIntake.setDirection(DcMotorSimple.Direction.REVERSE);

        initVuforia();

        if (ClassFactory.getInstance().canCreateTFObjectDetector()) {
            initTfod();
        } else {
            opMode.telemetry.addData("Sorry!", "This device is not compatible with TFOD");
        }

        _leftOffset = leftDrive.getCurrentPosition();
        _rightOffset = rightDrive.getCurrentPosition();
    }

    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the Tensor Flow Object Detection engine.
    }

    public void initTFOD(){
        tfod.activate();
    }

    /**
     * Initialize the Tensor Flow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hwMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hwMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABEL_GOLD_MINERAL, LABEL_SILVER_MINERAL);
    }

    //Returns a value depending on the position of the mineral, 0 is right most, 1 is middle, 2 is leftmost, -1 is an error

    public int getGoldPos(double timeThreshold){

        int position = 0;

        while (time.seconds() < timeThreshold){
            opMode.telemetry.addData("pos", position);
            opMode.telemetry.update();
            List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
            if (updatedRecognitions != null) {
                for (Recognition recognition : updatedRecognitions) {
                    if (recognition.getLabel().equals(LABEL_GOLD_MINERAL) && recognition.getLeft() > goldThreshold) {
                        position = 2;
                    } else if (recognition.getLabel().equals(LABEL_GOLD_MINERAL)) {
                        position = 1;
                    } else position = 0;
                }
            }
        }
        tfod.deactivate();
        return position;
    }

    //Sets the power of both sides of the bot
    public void setPower(double leftPower, double rightPower) {
        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);
    }

    public void setLiftPower(double liftPower) {
        liftArm.setPower(liftPower);
    }

    public void setBehavior(DcMotor.ZeroPowerBehavior behavior){
        leftDrive.setZeroPowerBehavior(behavior);
        rightDrive.setZeroPowerBehavior(behavior);
    }

    public void stopDrive(){
        setPower(0,0);

    }

    public void setLeftDirection(DcMotor.Direction direction) {
        leftDrive.setDirection(direction);
    }

    public void setRightDirection(DcMotor.Direction direction) {
        leftDrive.setDirection(direction);
    }

    public int getRightPosition()
    {
        return rightDrive.getCurrentPosition();
    }

    public int getLeftPosition()
    {
        return leftDrive.getCurrentPosition();
    }


    public void driveStraight(double inches)
    {
        driveStraight(opMode, inches, AUTO_DRIVE_SPEED, P_DRIVE_COEFF);
    }
    public void driveStraight(double inches, double speed)
    {
        driveStraight(opMode, inches, speed, P_DRIVE_COEFF);
    }

    /**
     * Method for driving straight
     *
     * @param inches Inches
     */

    public void driveStraight(LinearOpMode opmode, double inches, double maxSpeed, double pCoeff) {
        double speed = 0;
        int error;
        //sets the target encoder value
        int target = rightDrive.getCurrentPosition() + (int) (inches / INCHES_PER_TICK);
        //sets current gyro value
        double startHeading = getGyroHeading();
        // While the absolute value of the error is greater than the error threshold
        //adds the f value if positive or subtracts if negative
        while (opmode.opModeIsActive() && Math.abs(rightDrive.getCurrentPosition() - target) >= DRIVE_THRESHOLD) {
            error = target - rightDrive.getCurrentPosition();
            if (error * pCoeff < 0) {
                speed = Range.clip((error * pCoeff) - F_MOTOR_COEFF, -maxSpeed, 0);
            } else {
                speed = Range.clip((error * pCoeff) + F_MOTOR_COEFF, 0, maxSpeed) ;
            }

            if (Math.abs(getGyroHeading() - startHeading) > 1){
                setPower(speed, speed + POWER_DAMPEN * (getGyroHeading() - startHeading));
            }
            else {setPower(speed, speed);}

            opmode.telemetry.addData("Drive Error", error);
            opmode.telemetry.addData("Drive Power", rightDrive.getPower());
            opMode.telemetry.update();
        }
        this.stopDrive();
    }



    /**
     * Checks if the gyro is calibrating
     *
     * @return isCalibrating
     */
    public boolean isGyroCalibrating() {
        boolean isCalibrating = !imu.isGyroCalibrated();

        return isCalibrating;
    }

    /**
     * Gets the heading of the gyro in degrees
     *
     * @return heading
     */
    public double getGyroHeading() {
        // Update gyro
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        gravity = imu.getGravity();

        double heading = AngleUnit.DEGREES.normalize(AngleUnit.DEGREES.fromUnit(angles.angleUnit, angles.firstAngle));
        return heading;
    }

    //parameter overloaders for gyroTurn
    public void gyroTurn(double angle)
    {
        gyroTurn(AUTO_TURN_SPEED, angle, TIMEOUT, P_TURN_COEFF);
    }

    public void gyroTurn(double angle, double timeOut)
    {
        gyroTurn(AUTO_TURN_SPEED, angle, timeOut, P_TURN_COEFF);
    }

    //Turns the bot using onHeading, times out if it does not finish
    public void gyroTurn(double speed, double angle, double timeOut, double pCoeff)
    {
        while(opMode.opModeIsActive() && !onHeading(speed, angle, pCoeff) && time.time() < timeOut)
        {
            opMode.telemetry.update();
        }
    }

    boolean onHeading ( double speed, double angle, double PCoeff){
        double error;
        double steer = 0;
        boolean onTarget = false;
        double leftSpeed = 0;
        double rightSpeed= 0;

        // determine turn power based on +/- error
        error = getError(angle);
        if(Math.abs(error) <= HEADING_THRESHOLD && time.time() >= HOLD_TIME && timerStarted) {
            steer = 0.0;
            leftSpeed = 0.0;
            rightSpeed = 0.0;
            onTarget = true;
        }

        else if (Math.abs(error) <= HEADING_THRESHOLD) {
            if (timerStarted == false) {
                time.reset();
                timerStarted = true;
                opMode.telemetry.addLine("Reset Time");
                steer = getSteer(error, PCoeff);
                steer = getSteer(error, PCoeff);
                if(steer < 0){
                    steer -= F_MOTOR_COEFF;
                }
                else{steer += F_MOTOR_COEFF;}
                rightSpeed = Range.clip(steer, -speed, speed);
                leftSpeed = -rightSpeed;
            }
            else{
                opMode.telemetry.addLine("Timer is running");
                steer = getSteer(error, PCoeff);
                steer = getSteer(error, PCoeff);
                if(steer < 0){
                    steer -= F_MOTOR_COEFF;
                }
                else{steer += F_MOTOR_COEFF;}
                rightSpeed = Range.clip(steer, -speed, speed);
                leftSpeed = -rightSpeed;
            }
        }

        else {
            steer = getSteer(error, PCoeff);
            if(steer < 0){
                steer -= F_MOTOR_COEFF;
            }
            else{steer += F_MOTOR_COEFF;}
            rightSpeed = Range.clip(steer, -speed, speed);
            leftSpeed = -rightSpeed;
            timerStarted = false;
        }
        // Send desired speeds to motors
        setPower(leftSpeed, rightSpeed);

        // Display it for the driver
        opMode.telemetry.addData("Target", "%5.2f", angle);
        opMode.telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
        opMode.telemetry.addData("Speed", "%5.2f:%5.2f", leftSpeed, rightSpeed);
        opMode.telemetry.addData("timer started", timerStarted);
        opMode.telemetry.addData("hold timer", time.time());
        opMode.telemetry.addData("on Target", onTarget);

        return onTarget;
    }
    /**
     * Gets the pitch of the gyro in degrees
     *
     * @return pitch
     */
    public double getGyroPitch() {
        // Update gyro
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        gravity = imu.getGravity();

        double pitch = AngleUnit.DEGREES.normalize(AngleUnit.DEGREES.fromUnit(angles.angleUnit, angles.thirdAngle));
        return pitch;
    }

        /**
         * Perform one cycle of closed loop heading control.
         *
         * speed  Desired speed of turn.
         * angle  Absolute Angle (in Degrees) relative to last gyro reset.
         *               0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *               If a relative angle is required, add/subtract from current heading.
         * PCoeff Proportional Gain coefficient
         * @return onTarget
         */
    public boolean isTimerStarted() {
        return timerStarted;
    }

    /**
     * getError determines the error between the target angle and the robot's current heading
     *
     * @param targetAngle Desired angle (relative to global reference established at last Gyro Reset).
     * @return error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
     * Positive error means the robot should turn LEFT (CCW) to reduce error.
     */
     public double getError ( double targetAngle){

        double robotError;

        // calculate error in -179 to +180 range  (
        robotError = targetAngle - getGyroHeading();
        while (robotError > 180) robotError -= 360;
        while (robotError <= -180) robotError += 360;
        return robotError;
     }

     /**
     * returns desired steering force.  +/- 1 range.  positive = steer left
     *
     * @param error  Error angle in robot relative degrees
     * @param PCoeff Proportional Gain Coefficient
     * @return steer
     */
     public double getSteer ( double error, double PCoeff){
        if (error * PCoeff < 0){
            return Range.clip((error * PCoeff)  - F_MOTOR_COEFF, -1, 0);
        }
        else{return Range.clip((error * PCoeff) + F_MOTOR_COEFF, 0, 1) ;}
     }

     public void resetTimer () {
        time.reset();
     }

     public ElapsedTime getTime () {
        return time;
     }

     public boolean isYellow(){
         float[] hsvValues = new float[3];
         Color.RGBToHSV(colorSensor.red() * 8, colorSensor.green() * 8, colorSensor.blue() * 8, hsvValues);
         return (hsvValues[0] > YELLOW_THRESHOLD) && (YELLOW_LIMIT > hsvValues[0] );
     }

     public void setServo (double position){
         servo.setPosition(position);
     }
     public double getServo () {
       return servo.getPosition();
    }
     public void setInPower (double inPower){
         //leftIntake.setPower(inPower);
         //rightIntake.setPower(inPower);
     }

     public void sleep (long millis){
         try {
             Thread.sleep(millis);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }

}
