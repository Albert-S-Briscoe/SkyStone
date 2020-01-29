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

            ArmDriver arm = new ArmDriver(hardwareMap);

        if you are going to run anything on another thread then add this
        at the top of runOpMode()

            ExecutorService pool = Executors.newFixedThreadPool(2);

        to call a function use this format

            arm.[function name]([var 1], [var 2], ...);

        info for individual functions are included at the top of the function

 */

public class ArmDriver implements Runnable {

    private final double COUNTS_PER_REVOLUTION = 288;
    private final double LITTLE_WHEEL_DIAMETER_INCHES = 4.0;
    private final double COUNTS_PER_INCH = COUNTS_PER_REVOLUTION / (LITTLE_WHEEL_DIAMETER_INCHES * 3.14159);
    private final double maxHieght = 15;
    private final double power = 0.5;

    public boolean moveDone = false;
    public boolean stop = false;
    private int target;
    private double inches = 0;

    RobotHardware H;

    ArmDriver(RobotHardware H) {

        this.H = H;

    }

    void init(HardwareMap HM) {

        /**initializes RobotHardware
         * requires hardwaremap as input
         */
        H.init(HM);
    }

    public void run() {
        /**
         * to run a function use either
         *
         *    arm.setMoveToDeg(double, double);
         *
         *    or
         *
         *    arm.setMoveToHeight();
         *
         * then run this to start it
         *    pool.execute(arm);
         *
         */

        double inches = 0;

        while (!stop) {

            if (inches != this.inches) {

                target = (int)(this.inches * COUNTS_PER_INCH);
                H.vertical.setTargetPosition(target);

            }

            moveDone = !H.vertical.isBusy();
            inches = this.inches;

        }
    }

    /*void setMoveToDeg(double MoveToAngle) {

        this.armTargetAngle = Range.clip(MoveToAngle, 0, 135);

        isToDeg = true;
        moveDone = false;
        stop = false;

    }

    private void MoveToDeg() {

         do {

             armAngle = (H.vertpos.getVoltage() - zeroVolts) * degreesPerVolt;
             armOffAngle = Math.abs(armAngle - armTargetAngle);

             if (armAngle > armTargetAngle) {

                 H.vertical.setPosition(1 - Range.clip(1 / armOffAngle, 0, .4));

             } else {

                 H.vertical.setPosition(0 + Range.clip(1 / armOffAngle, 0, .4));

             }

        } while (armOffAngle > 2.5 && !stop);

        H.vertical.setPosition(0.5);

    }*/

    public void moveToInch(double inches) {

        this.inches = Range.clip(inches, 0, maxHieght);
        moveDone = false;
        stop = false;

    }

    private void moveToBlock(byte blocknum) {

        inches = Range.clip(((blocknum - 1) * 4) + 2.5, 0, maxHieght);
        moveDone = false;
        stop = false;

    }

    void zero() {

        H.vertical.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        H.vertical.setMode(DcMotor.RunMode.RUN_TO_POSITION);

    }

    void stop() {

        stop = true;

    }

    void pause() {

        H.vertical.setPower(0);

    }

    void play() {

        H.vertical.setPower(power);

    }

}
