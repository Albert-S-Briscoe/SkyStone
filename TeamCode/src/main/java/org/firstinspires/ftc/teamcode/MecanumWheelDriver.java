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

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

/**     put the following code inside runOpMode()

            MecanumWheelDriver drive = new MecanumWheelDriver(hardwareMap);

        if you are going to run using encoders then include this as well

            drive.RunWithEncoders(true);

        if you are going to run anything on another thread then add this
        at the top of runOpMode()

            ExecutorService pool = Executors.newFixedThreadPool(1);

        to call a function use this format

            drive.[function name]([var 1], [var 2], ...);

        info for individual functions are included at the top of the function


        All degrees inputs are in this format

            0
         90   -90
           180
 */

public class MecanumWheelDriver implements Runnable {

    private int Angle_Degrees;
    private double inches;
    private double speed;
    private double agl_frwd;

    private int Degrees;
    private double MaxSpeed;
    private boolean toDegree;

    private byte moveMode; // 1 = moveInches(), 2 = rotate(), 3 = MoveArmToDeg()
    public boolean moveDone = false;

    private final double COUNTS_PER_REVOLUTION = 288;
    private final double WHEEL_DIAMETER_INCHES = 4.0;     // For figuring circumference
    //final double ROBOT_DIAMETER_INCHES = 23;
    private final double COUNTS_PER_INCH = COUNTS_PER_REVOLUTION / (WHEEL_DIAMETER_INCHES * 3.14159);
    //final double COUNTS_PER_DEGREE = ((ROBOT_DIAMETER_INCHES * 3.14159) / 360) * COUNTS_PER_INCH;

    private final double turnAccrate = 1;
    private double speedmin = 0.15;
    private int rampDownAngl = 50;
    boolean selfcorrect = true;

    private final double zeroVolts = 0.38;
    private final double degreesPerVolt = 111;
    private double maxArmPower = 0.3;
    private double armTargetAngle;
    private double armAngle;
    private double armOffAngle;

    HardwareMap HM;

    RobotHardware H = new RobotHardware();

    void init(HardwareMap HM) {

        /**initializes RobotHardware
         * requires hardwaremap as input
         */
        H.init(HM);
    }

    public void run() {
        /**
         * to run a function use either
         *    drive.setrotate(int, double, boolean);
         * or
         *    drive.setMoveInches(int, double, double, double);
         * then run this to start it
         *    pool.execute(drive);
         *
         * this is only for MoveInches(), Rotate() and MoveArmToDeg() functions
         */

        switch (moveMode) {
            case 1:
                MoveInches(/*Angle_Degrees, inches, speed, agl_frwd*/);
                break;
            case 2:
                rotate(/*Degrees, MaxSpeed, toDegree*/);
                break;
            case 3:
                MoveArmToDeg();
                break;
        }

        moveDone = true;
    }

    void move(double Angle_Degrees, double Speed, double Rotate) {

        /**Angle_Degrees, the angle relative to the robot that it should move
         * Speed, the speed at which to run the motors
         * Rotate, used to rotate the robot. can be set while moving
         *
         * move() will run motors until another function that changes motor speed is called
         * you can use stop() to stop the motors
         *
         * if you want to move backwards you either set speed negative or Angle_degrees to 180
         */

        double leftfrontPower;
        double rightfrontPower;
        double leftbackPower;
        double rightbackPower;

        double rTotal = Speed + Math.abs(Rotate);
        double Angle = Math.toRadians(Angle_Degrees + 45);
        double cosAngle = Math.cos(Angle);
        double sinAngle = Math.sin(Angle);
        double LF_RB;  //leftfront and rightback motors
        double RF_LB;  //rightfront and leftback motors
        double multiplier;

        if (Math.abs(cosAngle) > Math.abs(sinAngle)) {   //scale the motor's speed so that at least one of them = 1
            multiplier = 1/Math.abs(cosAngle);
            LF_RB = multiplier * cosAngle;
            RF_LB = multiplier * sinAngle;
        } else {
            multiplier = 1/Math.abs(sinAngle);
            LF_RB = multiplier * cosAngle;
            RF_LB = multiplier * sinAngle;
        }

        leftfrontPower    = LF_RB * Speed + Rotate;
        rightfrontPower   = RF_LB * Speed - Rotate;
        leftbackPower     = RF_LB * Speed + Rotate;
        rightbackPower    = LF_RB * Speed - Rotate;

        if (Math.abs(rTotal) > 1) {
            leftfrontPower    = leftfrontPower/rTotal;
            rightfrontPower   = rightfrontPower/rTotal;
            leftbackPower     = leftbackPower/rTotal;
            rightbackPower    = rightbackPower/rTotal;
        }

        H.leftfront.  setPower(leftfrontPower);
        H.rightfront. setPower(rightfrontPower);
        H.leftback.   setPower(leftbackPower);
        H.rightback.  setPower(rightbackPower);
    }

    void moveWithGyro(double Angle_Degrees, double Speed, double agl_frwd) {

        /**Angle_Degrees, the angle relative to the robot that it should move
         * Speed, the speed at which to run the motors
         * Rotate, used to rotate the robot. can be set while moving
         *
         * move() will run motors until another function that changes motor speed is called
         * you can use stop() to stop the motors
         *
         * if you want to move backwards you either set speed negative or Angle_degrees to 180
         */

        double leftfrontPower;
        double rightfrontPower;
        double leftbackPower;
        double rightbackPower;

        double Angle = Math.toRadians(Angle_Degrees + 45);
        double cosAngle = Math.cos(Angle);
        double sinAngle = Math.sin(Angle);
        double LF_RB;  //leftfront and rightback motors
        double RF_LB;  //rightfront and leftback motors
        double multiplier;
        double offset;

        if (agl_frwd == -1) {
            agl_frwd = H.getheading();
        }

        if (Math.abs(cosAngle) > Math.abs(sinAngle)) {   //scale the motor's speed so that at least one of them = 1
            multiplier = 1/Math.abs(cosAngle);
            LF_RB = multiplier * cosAngle;
            RF_LB = multiplier * sinAngle;
        } else {
            multiplier = 1/Math.abs(sinAngle);
            LF_RB = multiplier * cosAngle;
            RF_LB = multiplier * sinAngle;
        }

        offset = FindDegOffset(H.getheading(), agl_frwd + 180);

        leftfrontPower = LF_RB - offset / 45;
        rightfrontPower = RF_LB + offset / 45;
        leftbackPower = RF_LB - offset / 45;
        rightbackPower = LF_RB + offset / 45;

        if (LF_RB > RF_LB) {
            if (offset > 0) {
                multiplier = 1 / Math.abs(rightbackPower);
            } else {
                multiplier = 1 / Math.abs(leftfrontPower);
            }
        } else {
            if (offset > 0) {
                multiplier = 1 / Math.abs(rightfrontPower);
            } else {
                multiplier = 1 / Math.abs(leftbackPower);
            }
        }

        H.leftfront.setPower(Range.clip(leftfrontPower * multiplier * Speed, -1, 1));
        H.rightfront.setPower(Range.clip(rightfrontPower * multiplier * Speed, -1, 1));
        H.leftback.setPower(Range.clip(leftbackPower * multiplier * Speed, -1, 1));
        H.rightback.setPower(Range.clip(rightbackPower * multiplier * Speed, -1, 1));
    }

    void stop() {

        /**stops all the motors
         */

        H.leftfront.  setPower(0);
        H.rightfront. setPower(0);
        H.leftback.   setPower(0);
        H.rightback.  setPower(0);
    }

    void setMoveInches(int Angle_Degrees, double inches, double speed, double agl_frwd) {

        this.Angle_Degrees = Angle_Degrees;
        this.inches = inches;
        this.speed = speed;
        this.agl_frwd = agl_frwd;

        moveMode = 1;
        moveDone = false;

    }

    void MoveInches(/*int Angle_Degrees, double inches, double speed, double agl_frwd*/) {

        /**Angle_Degrees, the angle relative to the robot that it should move
         * inches, the number of inches to move. Is not accurate when going sideways due to the mecanum wheels
         * speed, the speed at which to run the motors
         * agl_frwd, the direction the robot should face while moving
         * it will be set automatically if = -1
         * forward = 0 degrees, right = 90, left = -90, back = 180
         *
         * DO NOT set speed to a negative number
         * if you want to move backwards set Angle_degrees to 180
         */

        double Angle = Math.toRadians(Angle_Degrees + 45);// - Math.PI / 4;
        double cosAngle = Math.cos(Angle);
        double sinAngle = Math.sin(Angle);
        double LF_RB;  //leftfront and rightback motors
        double RF_LB;  //rightfront and leftback motors
        double multiplier;
        double offset;
        double leftfrontPower;
        double rightfrontPower;
        double leftbackPower;
        double rightbackPower;
        selfcorrect = true;

        if (agl_frwd == -1) {
            agl_frwd = H.getheading() - 180;
            selfcorrect = true;
        } else if (agl_frwd == -2) {
            selfcorrect = false;
        }

        if (Math.abs(cosAngle) > Math.abs(sinAngle)) {   //scale the motor's speed so that at least one of them = 1
            multiplier = 1 / Math.abs(cosAngle);
            LF_RB = multiplier * cosAngle;
            RF_LB = multiplier * sinAngle;
        } else {
            multiplier = 1 / Math.abs(sinAngle);
            LF_RB = multiplier * cosAngle;
            RF_LB = multiplier * sinAngle;
        }

        int LF_RBtarget = (int)(LF_RB * inches * COUNTS_PER_INCH);
        int RF_LBtarget = (int)(RF_LB * inches * COUNTS_PER_INCH);

        H.leftfront.  setTargetPosition(H.leftfront.  getCurrentPosition() + LF_RBtarget);
        H.rightfront. setTargetPosition(H.rightfront. getCurrentPosition() + RF_LBtarget);
        H.leftback.   setTargetPosition(H.leftback.   getCurrentPosition() + RF_LBtarget);
        H.rightback.  setTargetPosition(H.rightback.  getCurrentPosition() + LF_RBtarget);

        H.leftfront.  setMode(DcMotor.RunMode.RUN_TO_POSITION);
        H.rightfront. setMode(DcMotor.RunMode.RUN_TO_POSITION);
        H.leftback.   setMode(DcMotor.RunMode.RUN_TO_POSITION);
        H.rightback.  setMode(DcMotor.RunMode.RUN_TO_POSITION);

        H.leftfront.  setPower(LF_RB * speed);
        H.rightfront. setPower(RF_LB * speed);
        H.leftback.   setPower(RF_LB * speed);
        H.rightback.  setPower(LF_RB * speed);

        while (H.leftfront.isBusy() && H.rightfront.isBusy() && H.leftback.isBusy() && H.rightback.isBusy()) {
            if (selfcorrect) {
                offset = FindDegOffset(H.getheading(), agl_frwd + 180);

                leftfrontPower = LF_RB * speed - offset / 45;
                rightfrontPower = RF_LB * speed + offset / 45;
                leftbackPower = RF_LB * speed - offset / 45;
                rightbackPower = LF_RB * speed + offset / 45;

                if (LF_RB > RF_LB) {
                    if (offset > 0) {
                        multiplier = 1 / Math.abs(rightbackPower);
                    } else {
                        multiplier = 1 / Math.abs(leftfrontPower);
                    }
                } else {
                    if (offset > 0) {
                        multiplier = 1 / Math.abs(rightfrontPower);
                    } else {
                        multiplier = 1 / Math.abs(leftbackPower);
                    }
                }

                H.leftfront.setPower(Range.clip(leftfrontPower * multiplier, -1, 1));
                H.rightfront.setPower(Range.clip(rightfrontPower * multiplier, -1, 1));
                H.leftback.setPower(Range.clip(leftbackPower * multiplier, -1, 1));
                H.rightback.setPower(Range.clip(rightbackPower * multiplier, -1, 1));
            }

        }

        H.leftfront.  setPower(0);
        H.rightfront. setPower(0);
        H.leftback.   setPower(0);
        H.rightback.  setPower(0);

        H.leftfront.  setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        H.rightfront. setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        H.leftback.   setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        H.rightback.  setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        /*while (H.leftfront.getCurrentPosition() != leftfrontTarget && H.rightfront.getCurrentPosition() != rightfrontTarget && H.leftback.getCurrentPosition() != leftbackTarget && H.rightback.getCurrentPosition() != rightbackTarget) {
            offset = FindDegOffset(H.getheading(), agl_frwd + 180);

            leftfrontPower = LF_RB * speed - offset/45;
            rightfrontPower = RF_LB * speed + offset/45;
            leftbackPower = RF_LB * speed - offset/45;
            rightbackPower = LF_RB * speed + offset/45;

            if (LF_RB > RF_LB) {
                if (offset > 0) {
                    multiplier = 1/Math.abs(rightbackPower);
                } else {
                    multiplier = 1/Math.abs(leftfrontPower);
                }
            } else {
                if (offset > 0) {
                    multiplier = 1/Math.abs(rightfrontPower);
                } else {
                    multiplier = 1/Math.abs(leftbackPower);
                }
            }

            H.leftfront.  setPower(Range.clip(leftfrontPower * multiplier, -1, 1));
            H.rightfront. setPower(Range.clip(rightfrontPower * multiplier, -1, 1));
            H.leftback.   setPower(Range.clip(leftbackPower * multiplier, -1, 1));
            H.rightback.  setPower(Range.clip(rightbackPower * multiplier, -1, 1));

        }

        H.leftfront.  setPower(0);
        H.rightfront. setPower(0);
        H.leftback.   setPower(0);
        H.rightback.  setPower(0);*/

    }

    void setrotate(int Degrees, double MaxSpeed, boolean toDegree) {
        this.Degrees = Degrees;
        this.MaxSpeed = MaxSpeed;
        this.toDegree = toDegree;

        moveMode = 2;
        moveDone = false;
    }

    void rotate(/*int Degrees, double MaxSpeed, boolean toDegree*/) {

        /**if toDegree is false:
         * Rotates a number of degrees relative to the robot's current angle
         * the robot will rotate in one direction until it arrives at the target degree
         * the direction is determined by whether or not the input degree is negative
         * -degree = right, +degree = left
         *
         * if toDegree is true:
         * Rotates to a degree relative to the starting angle of the robot
         * for example: rotateToDeg(-90, 1); would make the robot turn at full speed to -90 degrees (right)
         * where 0 degrees is the front of the robot when the RobotHardware is initialized
         * rotateToDeg will always find the shortest direction to turn even while running
         * it will never turn more than 180 degrees
         * while running the robot can be intercepted and will still stop at the target degrees
         *
         * Both:
         * a number of degrees before the target the robot will gradually slow down to the min speed
         * both can be set at the top of the class as rampDownAngl and speedmin or with the setRampDown() function
         * DO NOT input a negative MaxSpeed if you do the robot won't move and will be stuck in an infinite loop
         */

        double speed;
        double offset;
        double heading = H.getheading();
        double Target;
        int drect;
        //H.angles   = H.imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        if (toDegree) {
            Target = Degrees + 180;
        } else {
            Target = addDegree(heading, Degrees);
        }

        do {
            heading = H.getheading();
            offset = FindDegOffset(heading, Target);
            speed = Range.clip( Math.abs(offset / rampDownAngl), speedmin, MaxSpeed);
            drect = (int)Range.clip(offset * 100, -1, 1);
            //H.angles   = H.imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

            H.leftfront.  setPower(-speed * drect);
            H.rightfront. setPower(speed * drect);
            H.leftback.   setPower(-speed * drect);
            H.rightback.  setPower(speed * drect);

        } while (Math.abs(offset) > turnAccrate);

        H.leftfront.  setPower(0);
        H.rightfront. setPower(0);
        H.leftback.   setPower(0);
        H.rightback.  setPower(0);

        /*
        int Lefttarget = (int)(Degrees * COUNTS_PER_DEGREE);
        int Righttarget = -((int)(Degrees * COUNTS_PER_DEGREE));

        leftfront.setTargetPosition(leftfront.getCurrentPosition() + Lefttarget);
        rightfront.setTargetPosition(rightfront.getCurrentPosition() + Righttarget);
        leftback.setTargetPosition(leftback.getCurrentPosition() + Lefttarget);
        rightback.setTargetPosition(rightback.getCurrentPosition() + Righttarget);

        leftfront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightfront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftback.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightback.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftfront.setPower(speed);
        rightfront.setPower(-speed);
        leftback.setPower(speed);
        rightback.setPower(-speed);

        while (leftfront.isBusy() && rightfront.isBusy() && leftback.isBusy() && rightback.isBusy()) {
            //idle();
        }

        leftfront.setPower(0);
        rightfront.setPower(0);
        leftback.setPower(0);
        rightback.setPower(0);

        leftfront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightfront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftback.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightback.setMode(DcMotor.RunMode.RUN_USING_ENCODER);*/
    }

    void setMoveArmToDeg(double armMove, double maxArmPower) {

        this.armTargetAngle = Range.clip(armMove, 0, 135);
        this.maxArmPower = maxArmPower;

        moveMode = 3;
        moveDone = false;

    }

    void MoveArmToDeg() {

         do {

             armAngle = (H.vertpos.getVoltage() - zeroVolts) * degreesPerVolt;
             armOffAngle = Math.abs(armAngle - armTargetAngle);

             if (armAngle > armTargetAngle) {

                 H.vertical.setPower(maxArmPower - Range.clip(1 / armOffAngle, 0, maxArmPower / 2 ));

             } else {

                 H.vertical.setPower(-maxArmPower + Range.clip(1 / armOffAngle, 0, maxArmPower / 2 ));

             }

        } while (armOffAngle > 5);

        H.vertical.setPower(0);

    }

    void setRampDown(int ramp_Down_Angle, double minimum_Speed) {
        /**sets what angle to start slowing down in rotateToDeg() and rotate()
         * and what the minimum speed should be, the robot may not go this speed if the ramp_Down_Angle is to low
         * default: ramp_Down_Angle = 50, minimum_Speed = 0.15 set either to 0 to reset to default
         * set ramp_Down_Angle to 1 for to not ramp down
         */
        if (ramp_Down_Angle <= 0) {
            rampDownAngl = 50;
        } else {
            rampDownAngl = ramp_Down_Angle;
        }
        if (minimum_Speed <= 0) {
            speedmin = 0.15;
        } else {
            speedmin = minimum_Speed;
        }
    }

    void RunWithEncoders(boolean On) {

        /**turns on or off encoders
         * will reset encoders when turned on
         * true = on, false = off
         */

        if (On) {
            H.leftfront.  setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            H.rightfront. setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            H.leftback.   setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            H.rightback.  setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

            H.leftfront.  setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            H.rightfront. setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            H.leftback.   setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            H.rightback.  setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        } else {
            H.leftfront.  setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            H.rightfront. setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            H.leftback.   setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            H.rightback.  setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    private double addDegree(double DegCurrent, double addDeg) {

        /**adds a number of degrees to the current degree with rapping around from 360 to 0
         * returns a value between 0 and 360
         */

        double output = DegCurrent + addDeg;
        while (output < 0 || output > 360) {
            if (output >= 360) {
                output -= 360;
            } else if (output < 0) {
                output += 360;
            }
        }
        return output;
    }

    private double FindDegOffset(double DegCurrent, double TargetDeg) {

        /**DegCurrent, the current degree of the robot value between 0 and 360
         * TargetDeg, the degree with which to find the offset
         * Finds the angle between current degree and the target degree
         * returns a value between -180 and 180
         * output will be negative if the current degree is left of the target, positive if on the right
         *    0
         * 90   -90
         *   180
         */

        double offset = TargetDeg - DegCurrent;
        if (offset > 180) {
            offset -= 360;
        } else if (offset < -180) {
            offset += 360;
        }
        return offset;
    }
}
