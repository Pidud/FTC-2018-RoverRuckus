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

package org.firstinspires.ftc.teamCode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

//Disabled
public class Bot {

    final static int ENCODER_TICKS_PER_REV = 1120;
    final static int WHEEL_DIAMETER = 4; //Inches
    final static double INCHES_PER_TICK = (WHEEL_DIAMETER * Math.PI) / ENCODER_TICKS_PER_REV;

    int _leftOffset;
    int _rightOffset;

    private final static double HEADING_THRESHOLD = 1; // As tight as we can make it with an integer gyro
    private final static double PITCH_THRESHOLD = 1; // As tight as we can make it with an integer gyro
    private final static double P_TURN_COEFF = 0.143;   // Larger is more responsive, but also less stable
    private final static double P_DRIVE_COEFF = 0.16;  // Larger is more responsive, but also less stable

    private final static double FLAT_PITCH = -1;    // Pitch when robot is flat on the balance stone
    private final static double BALANCE_PITCH = -8; // Pitch when robot is leaving the balance stone

    private final static double AUTO_DRIVE_SPEED = 0.6;
    private final static double AUTO_TURN_SPEED = 0.6;

    private DcMotor leftBackDrive = null;
    private DcMotor leftFrontDrive = null;
    private DcMotor rightFrontDrive = null;
    private DcMotor rightBackDrive = null;

    private LinearOpMode opMode = null;

    private HardwareMap hwMap = null;

    private BNO055IMU imu = null;
    private Orientation angles = null;
    private Acceleration gravity = null;
    private LinearOpMode opmode = null;

    private ElapsedTime time = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);

    public Bot(LinearOpMode opMode) {
        this.opMode = opMode;
    }

    public void init(HardwareMap ahwMap) {
        hwMap = ahwMap;

        leftBackDrive = hwMap.get(DcMotor.class, "backLeft");
        leftFrontDrive = hwMap.get(DcMotor.class, "frontLeft");
        rightBackDrive = hwMap.get(DcMotor.class, "backRight");
        rightFrontDrive = hwMap.get(DcMotor.class, "frontRight");

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = false;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu = hwMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        setLeftDirection(DcMotor.Direction.REVERSE);

        _leftOffset = leftFrontDrive.getCurrentPosition();
        _rightOffset = rightFrontDrive.getCurrentPosition();
    }

    public void setPower(double leftPower, double rightPower) {
        leftBackDrive.setPower(leftPower);
        leftFrontDrive.setPower(leftPower);
        rightBackDrive.setPower(rightPower);
        rightFrontDrive.setPower(rightPower);
    }

    public void setLeftDirection(DcMotor.Direction direction) {
        leftBackDrive.setDirection(direction);
        leftFrontDrive.setDirection(direction);
    }

    public void setRightDirection(DcMotor.Direction direction) {
        rightBackDrive.setDirection(direction);
        rightFrontDrive.setDirection(direction);
    }

    public int getRightPosition()
    {
        return rightFrontDrive.getCurrentPosition();
    }

    public int getLeftPosition()
    {
        return leftFrontDrive.getCurrentPosition();
    }

    public void driveStraight(double inches) {
        leftBackDrive.setTargetPosition((int) (inches / INCHES_PER_TICK));
        leftFrontDrive.setTargetPosition((int) (inches / INCHES_PER_TICK));
        rightBackDrive.setTargetPosition((int) (inches / INCHES_PER_TICK));
        rightFrontDrive.setTargetPosition((int) (inches / INCHES_PER_TICK));
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

    public void gyroTurn ( double angle){
        gyroTurn(AUTO_TURN_SPEED, angle);
    }

        /**
         * Method to spin on central axis to point in a new direction.
         * Move will stop if either of these conditions occur:
         * 1) Move gets to the heading (angle)
         * 2) Driver stops the opmode running.
         *
         * @param speed Desired speed of turn.
         * @param angle Absolute Angle (in Degrees) relative to last gyro reset.
         *              0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *              If a relative angle is required, add/subtract from current heading.
         */
        public void gyroTurn ( double speed, double angle){

            // keep looping while we are still active, and not on heading.
            while (opmode.opModeIsActive() && !onHeading(speed, angle, P_TURN_COEFF)) {
                // Update telemetry & Allow time for other processes to run.
                opmode.telemetry.update();
            }
        }

        public void gyroHold ( double angle, double holdTime){
            gyroHold(AUTO_TURN_SPEED, angle, holdTime);
        }

        /**
         * Method to obtain & hold a heading for a finite amount of time
         * Move will stop once the requested time has elapsed
         *
         * @param speed    Desired speed of turn.
         * @param angle    Absolute Angle (in Degrees) relative to last gyro reset.
         *                 0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *                 If a relative angle is required, add/subtract from current heading.
         * @param holdTime Length of time (in seconds) to hold the specified heading.
         */
        public void gyroHold ( double speed, double angle, double holdTime){

            ElapsedTime holdTimer = new ElapsedTime();

            // keep looping while we have time remaining.
            holdTimer.reset();
            while (opmode.opModeIsActive() && (holdTimer.time() < holdTime)) {
                // Update telemetry & Allow time for other processes to run.
                onHeading(speed, angle, P_TURN_COEFF);
                opmode.telemetry.update();
            }

            // Stop all motion;
            setPower(0, 0);
        }

        /**
         * Perform one cycle of closed loop heading control.
         *
         * @param speed  Desired speed of turn.
         * @param angle  Absolute Angle (in Degrees) relative to last gyro reset.
         *               0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *               If a relative angle is required, add/subtract from current heading.
         * @param PCoeff Proportional Gain coefficient
         * @return onTarget
         */
        boolean onHeading ( double speed, double angle, double PCoeff){
            double error;
            double steer;
            boolean onTarget = false;
            double leftSpeed;
            double rightSpeed;

            // determine turn power based on +/- error
            error = getError(angle);

            if (Math.abs(error) <= HEADING_THRESHOLD) {
                steer = 0.0;
                leftSpeed = 0.0;
                rightSpeed = 0.0;
                onTarget = true;
            } else {
                steer = getSteer(error, PCoeff);
                leftSpeed = speed * steer;
                rightSpeed = -leftSpeed;
            }

            // Send desired speeds to motors
            setPower(leftSpeed, rightSpeed);

            // Display it for the driver
            opmode.telemetry.addData("Target", "%5.2f", angle);
            opmode.telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
            opmode.telemetry.addData("Speed", "%5.2f:%5.2f", leftSpeed, rightSpeed);

            return onTarget;
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
            return Range.clip(error * PCoeff, -1, 1);
        }

        public void resetTimer () {
            time.reset();
        }

        public ElapsedTime getTime () {
            return time;
        }
}