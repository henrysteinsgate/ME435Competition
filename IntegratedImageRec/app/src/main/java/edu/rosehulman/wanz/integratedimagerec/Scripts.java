package edu.rosehulman.wanz.integratedimagerec;

import android.os.Handler;
import android.widget.Toast;

import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

/**
 * Created by wanz on 4/24/2018.
 */

public class Scripts {
    private Handler mCommandHandler = new Handler();
    private GolfBallDeliveryActivity mActivity;
    private int ARM_REMOVAL_TIME = 15000;

    public Scripts(GolfBallDeliveryActivity activity) {
        mActivity = activity;

    }

    public void testStraightScript() {
        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
        Toast.makeText(mActivity, "Begin driving", Toast.LENGTH_SHORT).show();
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.sendWheelSpeed(0, 0);
                Toast.makeText(mActivity, "Stop driving", Toast.LENGTH_SHORT).show();
            }
        }, 8000);
    }

    public void nearBallScript() {
//        double distanceToNearBall = NavUtils.getDistance(15, 0, 90, 50);
//        long driveTimeMs = (long) (distanceToNearBall / RobotActivity.DEFAULT_SPEED_FT_PER_SEC * 1000);

        // TODO: For testing, this has been made shorter
//        driveTimeMs = 3000;
//        mActivity.sendWheelSpeed(mActivity.mLeftStraightPwmValue, mActivity.mRightStraightPwmValue);
//        mCommandHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mActivity.sendWheelSpeed(0,0);
        removeBallAtLocation(mActivity.mNearBallLocation);
//
//            }
//        }, driveTimeMs);
//        mCommandHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARDS_FAR_BALL);
//            }
//        }, driveTimeMs + ARM_REMOVAL_TIME);

    }

    public void farBallScript() {
        mActivity.sendWheelSpeed(0, 0);
//        Toast.makeText(mActivity, "Figure out which ball(s) to remove and do it.", Toast.LENGTH_SHORT).show();
        removeBallAtLocation(mActivity.mFarBallLocation);
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity.mWhiteBallLocation != 0) {
                    removeBallAtLocation(mActivity.mWhiteBallLocation);
                }
                if (mActivity.mState == GolfBallDeliveryActivity.State.DROPBALL && mActivity.mStateCount == 2) {
                    mActivity.setState(GolfBallDeliveryActivity.State.DRIVE_TOWARDS_HOME);
                }
            }
        }, ARM_REMOVAL_TIME);

    }

    public void removeBallAtLocation(final int location) {
        mActivity.sendCommand("ATTACH 111111");
        if (location == 1) {
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.oneTwo);
                }
            }, 10);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.oneOff);
                }
            }, 5000);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.oneRecover);
                }
            }, 10000);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                    mActivity.sendCommand(mActivity.home);
                    mActivity.readyForNextState = 1;
                }
            }, 15000);
        } else if (location == 2) {
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.oneTwo);
                }
            }, 10);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.twoOff);
                }
            }, 5000);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                    mActivity.sendCommand(mActivity.home);
                    mActivity.readyForNextState = 1;
                }
            }, 10000);
        } else if (location == 3) {
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.twoThree);
                }
            }, 10);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.threeOff);
                }
            }, 5000);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.sendCommand(mActivity.threeRecover);
                }
            }, 10000);
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.setLocationToColor(location, GolfBallDeliveryActivity.BallColor.NONE);
                    mActivity.sendCommand(mActivity.home);
                    mActivity.readyForNextState = 1;
                }
            }, 15000);
        }

    }
}
