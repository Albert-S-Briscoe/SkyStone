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

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TeleOp(name="TeleOp Lite", group="Linear Opmode")
public class TeleOpLite extends LinearOpMode {

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        telemetry.update();
        ////////////////////////////// Init //////////////////////////////

        ElapsedTime         runtime = new ElapsedTime();
        MecanumWheelDriver drive = new MecanumWheelDriver();
        drive.init(hardwareMap);
        ExecutorService pool = Executors.newFixedThreadPool(1);
        RobotHardware H = new RobotHardware();
        H.init(hardwareMap);

        ////////////////////////////// Init Variables //////////////////////////////

        double leftfrontPower;
        double rightfrontPower;
        double leftbackPower;
        double rightbackPower;

        double LF_RB;  //leftfront and rightback motors
        double RF_LB;  //rightfront and leftback motors

        final double logCurve = 35;
        final double zeroVolts = 0.38;
        final double degreesPerVolt = 111;
        final double armLength = 11.75;
        final double armOffset = 6.5;
        final double rampDownAngle = 60;

        double y;
        double x;
        double Rotate;
        double Radius;
        double stickTotal;
        double multiplier;
        double Angle;
        double cosAngle;
        double sinAngle;

        double armAngle;
        double grabberToRobot;
        double previousArmPos = Math.cos(Math.toRadians((H.vertpos.getVoltage() - zeroVolts) * degreesPerVolt)) * armLength - armOffset;
        double inchesToCompensate;

        boolean slowDown = false;
        boolean speedButton = false;
        boolean useCompass = true;
        boolean compassButton = false;
        boolean grabberPosButton = false;
        double  position = 0;

        double agl_frwd = 180;
        double heading = 0;

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {

            ////////////////////////////// Set Variables //////////////////////////////

            y = -gamepad1.left_stick_y;
            x = gamepad1.left_stick_x;

            if (Math.abs(gamepad1.right_stick_x) > 0.05) {

                if (gamepad1.right_stick_x > 0) {

                    Rotate = -((Math.log10((-Range.clip(gamepad1.right_stick_x, -1, 1) + 1) * logCurve + 1)) / Math.log10(logCurve + 1)) * 0.85 + 1;

                } else {

                    Rotate = -(-((Math.log10((Range.clip(gamepad1.right_stick_x, -1, 1) + 1) * logCurve + 1)) / Math.log10(logCurve + 1)) * 0.85 + 1);

                }

            } else {

                Rotate = 0;

            }

            if (Math.hypot(x, y) > 0.05) {

                Radius = -((Math.log10((-Range.clip(Math.hypot(x, y), -1, 1) + 1) * logCurve + 1)) / Math.log10(logCurve + 1)) * 0.85 + 1;

            } else {

                Radius = 0;

            }

            stickTotal = Radius + Math.abs(Rotate);
            Angle = Math.atan2(y, x) + Math.toRadians(agl_frwd - heading - 45);
            cosAngle = Math.cos(Angle);
            sinAngle = Math.sin(Angle);

            armAngle = (H.vertpos.getVoltage() - zeroVolts) * degreesPerVolt;
            grabberToRobot = Math.cos(Math.toRadians(armAngle)) * armLength - armOffset;

            ////////////////////////////// Compensate For Arm //////////////////////////////

            if (stickTotal == 0) {

                if (drive.moveDone) {

                    inchesToCompensate = grabberToRobot - previousArmPos;
                    drive.setMoveInches(0, -inchesToCompensate, 0.8, -1);
                    pool.execute(drive);
                    previousArmPos = grabberToRobot;

                }

            } else if (!drive.moveDone) {

                pool.shutdownNow();
                drive.setMoveInches(0, 0, 0, -2);

            }

            ////////////////////////////// Mecanum Wheel Stuff //////////////////////////////

            if (Math.abs(cosAngle) > Math.abs(sinAngle)) {   //scale the motor's speed so that at least one of them = 1

                multiplier = 1 / Math.abs(cosAngle);
                LF_RB = multiplier * cosAngle;
                RF_LB = multiplier * sinAngle;

            } else {

                multiplier = 1 / Math.abs(sinAngle);
                LF_RB = multiplier * cosAngle;
                RF_LB = multiplier * sinAngle;

            }

            leftfrontPower = LF_RB * Radius + Rotate; //then add the rotate speed
            rightfrontPower = RF_LB * Radius - Rotate;
            leftbackPower = RF_LB * Radius + Rotate;
            rightbackPower = LF_RB * Radius - Rotate;

            if (Math.abs(stickTotal) > 1) {

                leftfrontPower = leftfrontPower / stickTotal;
                rightfrontPower = rightfrontPower / stickTotal;
                leftbackPower = leftbackPower / stickTotal;
                rightbackPower = rightbackPower / stickTotal;

            }

            ////////////////////////////// Move Arm //////////////////////////////

                H.vertical.setPower(Range.clip(gamepad1.left_trigger, 0, armAngle / rampDownAngle) - Range.clip(gamepad1.right_trigger, 0, (135 - armAngle) / rampDownAngle));


            ////////////////////////////// Buttons //////////////////////////////

            if (gamepad1.right_bumper) {

                if (!grabberPosButton) {

                    if (position == 0) {

                        position = 1;

                    } else {

                        position = 0;

                    }

                    grabberPosButton = true;

                }

            } else {

                grabberPosButton = false;

            }

            H.grabber.setPosition(position);

            if (gamepad1.dpad_up) {

                H.grab(false);

            } else if (gamepad1.dpad_down) {

                H.grab(true);

            }

            if (gamepad1.back) {

                if (!compassButton) {

                    useCompass = !useCompass;
                    compassButton = true;

                }

            } else {

                compassButton = false;

            }

            if (useCompass) {

                heading = H.getheading();

            } else {

                heading = agl_frwd;

            }

            if (gamepad1.start && stickTotal < 0.1) {

                agl_frwd = heading;

            }

            if (gamepad1.left_bumper | gamepad1.left_stick_button) {

                if (!speedButton) {

                    speedButton = true;
                    slowDown = !slowDown;

                }

            } else {

                speedButton = false;

            }

            if (slowDown) {

                H.leftfront.setPower(leftfrontPower / 2);
                H.rightfront.setPower(rightfrontPower / 2);
                H.leftback.setPower(leftbackPower / 2);
                H.rightback.setPower(rightbackPower / 2);

            } else {

                H.leftfront.setPower(leftfrontPower);
                H.rightfront.setPower(rightfrontPower);
                H.leftback.setPower(leftbackPower);
                H.rightback.setPower(rightbackPower);

            }

        }

        pool.shutdownNow();
        drive.setMoveInches(0, 0, 0, -2);

    }

}