package edu.rosehulman.golfballdelivery;

import android.os.Handler;
import android.widget.Toast;

import edu.rosehulman.golfballdelivery.GolfBallDeliveryActivity.BallColor;
import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

public class Scripts {

    /** Reference to the primary activity. */
    private GolfBallDeliveryActivity mActivity;

    /** Handler used to create scripts in this class. */
    protected Handler mCommandHandler = new Handler();

    /** Time in milliseconds needed to perform a ball removal. */
    private int ARM_REMOVAL_TIME_MS = 3000;

    /** Simple constructor. */
    public Scripts(GolfBallDeliveryActivity golfBallDeliveryActivity) {
        mActivity = golfBallDeliveryActivity;
    }

    /** Used to test your values for straight driving. */
    public void testStraightDriveScript() {
        Toast.makeText(mActivity, "Begin Short straight drive test at " +
                        mActivity.mLeftStraightPwmValue + "  " + mActivity.mRightStraightPwmValue,
                Toast.LENGTH_SHORT).show();
        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity, "End Short straight drive test", Toast.LENGTH_SHORT).show();
                mActivity.sendWheelSpeed(0, 0);
            }
        }, 8000);
    }

    /** Runs the script to drive to the near ball (perfectly straight) and drop it off. */
    public void nearBallScript() {
        Toast.makeText(mActivity, "Drive 103 ft to near ball.", Toast.LENGTH_SHORT).show();
        double distanceToNearBall = NavUtils.getDistance(15, 0, 90, 50);
        long driveTimeToNearBallMs = (long) (distanceToNearBall / RobotActivity.DEFAULT_SPEED_FT_PER_SEC * 1000);
        // Well done with the math, but now let’s cheat
        driveTimeToNearBallMs = 3000; // Make this mock script not take so long.
        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeBallAtLocation(mActivity.mNearBallLocation);
            }
        }, driveTimeToNearBallMs);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity.mState == GolfBallDeliveryActivity.State.NEAR_BALL_SCRIPT) {
                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARDS_FAR_BALL);
                }
            }
        }, driveTimeToNearBallMs + ARM_REMOVAL_TIME_MS);
    }


    /** Script to drop off the far ball. */
    public void farBallScript() {
        mActivity.sendWheelSpeed(0, 0);
        Toast.makeText(mActivity, "Figure out which ball(s) to remove and do it.", Toast.LENGTH_SHORT).show();
        removeBallAtLocation(mActivity.mFarBallLocation);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity.mWhiteBallLocation != 0) {
                    removeBallAtLocation(mActivity.mWhiteBallLocation);
                }
                if (mActivity.mState == GolfBallDeliveryActivity.State.FAR_BALL_SCRIPT) {
                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARDS_HOME);
                }
            }
        }, ARM_REMOVAL_TIME_MS);
    }


    // -------------------------------- Arm script(s) ----------------------------------------

    /** Removes a ball from the golf ball stand. */
    public void removeBallAtLocation(final int location) {
        // TODO: Replace with a script that might actually remove a ball. :)
        mActivity.sendCommand("ATTACH 111111"); // Just in case
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.sendCommand("POSITION 83 90 0 -90 90");
            }
        }, 10);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.sendCommand("POSITION 90 141 -60 -180 169");
            }
        }, 2000);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.setLocationToColor(location, BallColor.NONE);
            }
        }, ARM_REMOVAL_TIME_MS);
    }
}
