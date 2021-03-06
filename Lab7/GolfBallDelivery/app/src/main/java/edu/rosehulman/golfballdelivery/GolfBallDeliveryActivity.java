package edu.rosehulman.golfballdelivery;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import edu.rosehulman.me435.FieldGps;
import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;

public class GolfBallDeliveryActivity extends RobotActivity {

    /** Constant used with logging that you'll see later. */
    public static final String TAG = "GolfBallDelivery";
    private double mAccuracy;



    public enum State {
        READY_FOR_MISSION,
        NEAR_BALL_SCRIPT,
        DRIVE_TOWARDS_FAR_BALL,
        FAR_BALL_SCRIPT,
        DRIVE_TOWARDS_HOME,
        WAITING_FOR_PICKUP,
        SEEKING_HOME, LAB7, DROPBALL,
    }

    public State mState;
    private Scripts mScripts;

    /**
     * An enum used for variables when a ball color needs to be referenced.
     */
    public enum BallColor {
        NONE, BLUE, RED, YELLOW, GREEN, BLACK, WHITE;

    }

    /**
     * An array (of size 3) that stores what color is present in each golf ball stand location.
     */
    public BallColor[] mLocationColors = new BallColor[]{BallColor.NONE, BallColor.NONE, BallColor.NONE};

    /**
     * Simple boolean that is updated when the Team button is pressed to switch teams.
     */
    public boolean mOnRedTeam = false;


    // ---------------------- UI References ----------------------
    /**
     * An array (of size 3) that keeps a reference to the 3 balls displayed on the UI.
     */
    private ImageButton[] mBallImageButtons;

    /**
     * References to the buttons on the UI that can change color.
     */
    private Button mTeamChangeButton, mGoOrMissionCompleteButton;

    /**
     * An array constants (of size 7) that keeps a reference to the different ball color images resources.
     */
    // Note, the order is important and must be the same throughout the app.
    private static final int[] BALL_DRAWABLE_RESOURCES = new int[]{R.drawable.none_ball, R.drawable.blue_ball,
            R.drawable.red_ball, R.drawable.yellow_ball, R.drawable.green_ball, R.drawable.black_ball, R.drawable.white_ball};

    /**
     * TextViews that can change values.
     */
    private TextView mCurrentStateTextView, mStateTimeTextView, mGpsInfoTextView, mSensorOrientationTextView,
            mGuessXYTextView, mLeftDutyCycleTextView, mRightDutyCycleTextView, mMatchTimeTextView, mAccuracyTextView;

    // ---------------------- End of UI References ----------------------


    // ---------------------- Mission strategy values ----------------------
    /** Constants for the known locations. */
    public static final long NEAR_BALL_GPS_X = 90;
    public static final long FAR_BALL_GPS_X = 240;


    /** Variables that will be either 50 or -50 depending on the balls we get. */
    private double mNearBallGpsY, mFarBallGpsY;

    /**
     * If that ball is present the values will be 1, 2, or 3.
     * If not present the value will be 0.
     * For example if we have the black ball, then mWhiteBallLocation will equal 0.
     */
    public int mNearBallLocation, mFarBallLocation, mWhiteBallLocation;
    // ----------------- End of mission strategy values ----------------------


    // ---------------------------- Timing area ------------------------------
    /**
     * Time when the state began (saved as the number of millisecond since epoch).
     */
    private long mStateStartTime;

    /**
     * Time when the match began, ie when Go! was pressed (saved as the number of millisecond since epoch).
     */
    private long mMatchStartTime;

    /**
     * Constant that holds the maximum length of the match (saved in milliseconds).
     */
    private long MATCH_LENGTH_MS = 300000; // 5 minutes in milliseconds (5 * 60 * 1000)
    // ----------------------- End of timing area --------------------------------


    // ---------------------------- Driving area ---------------------------------
    /**
     * When driving towards a target, using a seek strategy, consider that state a success when the
     * GPS distance to the target is less than (or equal to) this value.
     */
    public static final double ACCEPTED_DISTANCE_AWAY_FT = 10.0; // Within 10 feet is close enough.

    /**
     * Multiplier used during seeking to calculate a PWM value based on the turn amount needed.
     */
    private static final double SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER = 0.8;  // units are (PWM value)/degrees

    /**
     * Variable used to cap the slowest PWM duty cycle used while seeking. Pick a value from -255 to 255.
     */
    private static final int LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE = 100;

    /**
     * PWM duty cycle values used with the drive straight dialog that make your robot drive straightest.
     */
    public int mLeftStraightPwmValue = 150, mRightStraightPwmValue = 150;
    // ------------------------ End of Driving area ------------------------------


    //------------------------- Start of Lab6BallSorterCode ----------------------
    private TextView mBall1, mBall2, mBall3;
    private int hundredth, tenth, oneth;
    private int loc1, loc2, loc3;
    private String message;
    private Handler mCommandHandler = new Handler();

    private String home = "POSITION 0 90 0 -90 90";

    private String oneTwo = "POSITION 21 125 -90 -160 108";
    private String oneOff = "POSITION 45 125 -90 -160 108";
    private String oneRecover = "POSITION 45 90 0 -90 90";

    private String twoThree = "POSITION -8 125 -90 -160 108";
    private String twoOff = "POSITION 8 125 -90 -160 108";

    private String threeOff = "POSITION -28 125 -90 -160 108";
    private String threeRecover = "POSITION -28 90 0 -90 90";
    //------------------------- End of Lab6BallSorterCode ------------------------

    public static int returnColor(BallColor mColor){
        if(mColor == BallColor.NONE) {
            return 0;
        } else if(mColor == BallColor.BLUE){
            return 1;
        }else if(mColor == BallColor.RED){
            return 2;
        }else if(mColor == BallColor.GREEN){
            return 3;
        }else if(mColor == BallColor.YELLOW){
            return 4;
        }else if(mColor == BallColor.BLACK){
            return 5;
        }else if(mColor == BallColor.WHITE){
            return 6;
        }else {
            return -1;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_golf_ball_delivery);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBallImageButtons = new ImageButton[]{(ImageButton) findViewById(R.id.location_1_image_button),
                (ImageButton) findViewById(R.id.location_2_image_button),
                (ImageButton) findViewById(R.id.location_3_image_button)};
        mTeamChangeButton = (Button) findViewById(R.id.team_change_button);
        mCurrentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        mStateTimeTextView = (TextView) findViewById(R.id.state_time_textview);
        mGpsInfoTextView = (TextView) findViewById(R.id.gps_info_textview);
        mSensorOrientationTextView = (TextView) findViewById(R.id.orientation_textview);
        mGuessXYTextView = (TextView) findViewById(R.id.guess_location_textview);
        mLeftDutyCycleTextView = (TextView) findViewById(R.id.left_duty_cycle_textview);
        mRightDutyCycleTextView = (TextView) findViewById(R.id.right_duty_cycle_textview);
        mMatchTimeTextView = (TextView) findViewById(R.id.match_time_textview);
        mGoOrMissionCompleteButton = (Button) findViewById(R.id.go_or_mission_complete_button);
        mAccuracyTextView = (TextView) findViewById(R.id.textView_accuracy);

        // When you start using the real hardware you don't need test buttons.
        boolean hideFakeGpsButtons = false;
        if (hideFakeGpsButtons) {
            TableLayout fakeGpsButtonTable = (TableLayout) findViewById(R.id.fake_gps_button_table);
            fakeGpsButtonTable.setVisibility(View.GONE);
        }


        setLocationToColor(1,BallColor.NONE);
        setLocationToColor(2,BallColor.NONE);
        setLocationToColor(3,BallColor.NONE);

        setState(State.READY_FOR_MISSION);
        mScripts = new Scripts(this);
    }


    // TODO: GPS first
    @Override
    protected void onStart() {
        super.onStart();
//        mFieldOrientation.registerListener(this);
        mFieldGps.requestLocationUpdates(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        mFieldOrientation.unregisterListener();
        mFieldGps.removeUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mFieldGps.requestLocationUpdates(this);
    }

    public void setState(State newState) {
        // Make sure when the match ends that no scheduled timer events from scripts change the FSM state.
//        if (mState == State.READY_FOR_MISSION && newState != State.NEAR_BALL_SCRIPT) {
//            Toast.makeText(this, "Illegal state transition out of READY_FOR_MISSION", Toast.LENGTH_SHORT).show();
//            return;
//        }
        mStateStartTime = System.currentTimeMillis();
        mCurrentStateTextView.setText(newState.name());
        speak(newState.name().replace("_", " ").toLowerCase());
        switch (newState) {
            case READY_FOR_MISSION:
                mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.green_button);
                mGoOrMissionCompleteButton.setText("Go!");
                sendWheelSpeed(0, 0);
                break;
            case NEAR_BALL_SCRIPT:
                mGpsInfoTextView.setText("---"); // Clear GPS display (optional)
                mGuessXYTextView.setText("---"); // Clear guess display (optional)

                break;
            case DRIVE_TOWARDS_FAR_BALL:
                // All actions handled in the loop function.
                break;
            case FAR_BALL_SCRIPT:

                break;
            case DRIVE_TOWARDS_HOME:
                // All actions handled in the loop function.
                break;
            case WAITING_FOR_PICKUP:
                sendWheelSpeed(0, 0);
                break;
            case SEEKING_HOME:
                // Actions handled in the loop function.
                break;
            case DROPBALL:
                sendCommand(oneTwo);
                sendWheelSpeed(0,0);
                break;

            case LAB7:

                break;
        }
        mState = newState;
    }


    /**
     * Use this helper method to set the color of a ball.
     * The location value here is 1 based.  Send 1, 2, or 3
     * Side effect: Updates the UI with the appropriate ball color resource image.
     */
    public void setLocationToColor(int location, BallColor ballColor) {
        mBallImageButtons[location - 1].setImageResource(BALL_DRAWABLE_RESOURCES[ballColor.ordinal()]);
        mLocationColors[location - 1] = ballColor;
    }

    /**
     * Used to get the state time in milliseconds.
     */
    private long getStateTimeMs() {
        return System.currentTimeMillis() - mStateStartTime;
    }

    /**
     * Used to get the match time in milliseconds.
     */
    private long getMatchTimeMs() {
        return System.currentTimeMillis() - mMatchStartTime;
    }


    // --------------------------- Methods added ---------------------------

    @Override
    public void loop() {
        super.loop();
        mStateTimeTextView.setText("" + getStateTimeMs() / 1000);
        mGuessXYTextView.setText("(" + (int) mGuessX + ", " + (int) mGuessY + ")");

//        Log.d(TAG, "This is loop within our subclass of Robot Activity");

        // Match timer.
        long timeRemainingSeconds = MATCH_LENGTH_MS / 1000;
        if (mState != State.READY_FOR_MISSION) {
            timeRemainingSeconds = (MATCH_LENGTH_MS - getMatchTimeMs()) / 1000;
            if (getMatchTimeMs() > MATCH_LENGTH_MS) {
                setState(State.READY_FOR_MISSION);
            }
        }
        mMatchTimeTextView.setText(getString(R.string.time_format, timeRemainingSeconds / 60, timeRemainingSeconds % 60));
        mAccuracyTextView.setText("" + (int)mAccuracy);
        switch (mState) {
//            case DRIVE_TOWARDS_FAR_BALL:
//                // TODO i was here
//                seekTargetAt(FAR_BALL_GPS_X,mFarBallGpsY);
//                break;
//            case DRIVE_TOWARDS_HOME:
//                seekTargetAt(0,0);
//                break;
//            case WAITING_FOR_PICKUP:
//                if (getStateTimeMs() > 8000) {
//                    setState(State.SEEKING_HOME);
//                }
//                break;
//            case SEEKING_HOME:
//                seekTargetAt(0,0);
//                if (getStateTimeMs() > 8000) {
//                    setState(State.WAITING_FOR_PICKUP);
//                }
//                break;
            case LAB7:
                if(minDistance(90,50,mGuessX,mGuessY) > 5) {
                    seekTargetAt(90, 50);
                }else {
                    setState(State.DROPBALL);
                    Toast.makeText(this,"You got it", Toast.LENGTH_SHORT).show();
                }
                break;
            case DROPBALL:

                if(getStateTimeMs() > 5000){
                    sendCommand(oneOff);
                }

                break;
            default:
                // Other states don't need to do anything, but could.
                break;
        }
    }

    private double minDistance(double targetX,double targetY,double currentX,double currentY){
        double xDifference = currentX - targetX;
        double yDifference = currentY - targetY;
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference);
    }

    private void seekTargetAt(double x, double y) {
        int leftDutyCycle = mLeftStraightPwmValue;
        int rightDutyCycle = mRightStraightPwmValue;
        double targetHeading = NavUtils.getTargetHeading(mGuessX, mGuessY, x, y);
        double leftTurnAmount = NavUtils.getLeftTurnHeadingDelta(mCurrentSensorHeading, targetHeading);
        double rightTurnAmount = NavUtils.getRightTurnHeadingDelta(mCurrentSensorHeading, targetHeading);
        if (leftTurnAmount < rightTurnAmount) {
            leftDutyCycle = mLeftStraightPwmValue - (int)(leftTurnAmount * SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER);
            leftDutyCycle = Math.max(leftDutyCycle, LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE);
//            leftDutyCycle = mLeftDutyCycle - (int)leftTurnAmount/180 * (leftDutyCycle - 100);
        } else {
            rightDutyCycle = mRightStraightPwmValue - (int)(rightTurnAmount * SEEKING_DUTY_CYCLE_PER_ANGLE_OFF_MULTIPLIER);
            rightDutyCycle = Math.max(rightDutyCycle, LOWEST_DESIRABLE_SEEKING_DUTY_CYCLE);
//            rightDutyCycle = mRightDutyCycle - (int)rightTurnAmount/180 * (rightDutyCycle - 100);
        }
        sendWheelSpeed(leftDutyCycle, rightDutyCycle);

    }

    private int changeByPercent(int input){
        return input/180 * (255 - 100);
    }


    // --------------------------- Drive command ---------------------------

    @Override
    public void sendWheelSpeed(int leftDutyCycle, int rightDutyCycle) {
        super.sendWheelSpeed(leftDutyCycle, rightDutyCycle); // Send the values to the
        mLeftDutyCycleTextView.setText("Left\n" + leftDutyCycle);
        mRightDutyCycleTextView.setText("Right\n" + rightDutyCycle);
    }


    // --------------------------- Sensor listeners ---------------------------

    @Override
    public void onLocationChanged(double x, double y, double heading, Location location) {
        super.onLocationChanged(x, y, heading, location);

        String gpsInfo = getString(R.string.xy_format, mCurrentGpsX, mCurrentGpsY);
        if (mCurrentGpsHeading != NO_HEADING) {
            gpsInfo += " " + getString(R.string.degrees_format, mCurrentGpsHeading);
        } else {
            gpsInfo += " ?º";
        }
        gpsInfo += "    " + mGpsCounter;
        mGpsInfoTextView.setText(gpsInfo);
        mAccuracy=location.getAccuracy() * FieldGps.FEET_PER_METER;


//        if (mState == State.DRIVE_TOWARDS_FAR_BALL) {
//            double distanceFromTarget = NavUtils.getDistance(mCurrentGpsX, mCurrentGpsY,
//                    FAR_BALL_GPS_X, mFarBallGpsY);
//            if (distanceFromTarget < ACCEPTED_DISTANCE_AWAY_FT) {
//                setState(State.FAR_BALL_SCRIPT);
//            }
//        }
//        if (mState == State.DRIVE_TOWARDS_HOME) {
//            // Shorter to write since the RobotActivity already calculates the distance to 0, 0.
//            if (mCurrentGpsDistance < ACCEPTED_DISTANCE_AWAY_FT) {
//                setState(State.WAITING_FOR_PICKUP);
//            }
//        }


    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues) {
        super.onSensorChanged(fieldHeading, orientationValues);
        mSensorOrientationTextView.setText(getString(R.string.degrees_format,
                mCurrentSensorHeading));
    }


    // --------------------------- Button Handlers ----------------------------

    /**
     * Helper method that is called by all three golf ball clicks.
     */
    private void handleBallClickForLocation(final int location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GolfBallDeliveryActivity.this);
        builder.setTitle("What was the real color?").setItems(R.array.ball_colors,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        GolfBallDeliveryActivity.this.setLocationToColor(location, BallColor.values()[which]);
                        if(location == 1){
                            GolfBallDeliveryActivity.this.loc1 = GolfBallDeliveryActivity.returnColor(BallColor.values()[which]);
                        } else if(location == 2){
                            GolfBallDeliveryActivity.this.loc2 = GolfBallDeliveryActivity.returnColor(BallColor.values()[which]);
                        } else if(location == 3) {
                            GolfBallDeliveryActivity.this.loc3 = GolfBallDeliveryActivity.returnColor(BallColor.values()[which]);
                        }
                    }
                });
        builder.create().show();

        // DONE: Fix this later so that you can manually change the ball color
    }

    /**
     * Click to the far left image button (Location 1).
     */
    public void handleBallAtLocation1Click(View view) {
        handleBallClickForLocation(1);
    }

    /**
     * Click to the center image button (Location 2).
     */
    public void handleBallAtLocation2Click(View view) {
        handleBallClickForLocation(2);
    }

    /**
     * Click to the far right image button (Location 3).
     */
    public void handleBallAtLocation3Click(View view) {
        handleBallClickForLocation(3);
    }

    /**
     * Sets the mOnRedTeam boolean value as appropriate
     * Side effects: Clears the balls
     * @param view
     */
    public void handleTeamChange(View view) {
        setLocationToColor(1, BallColor.NONE);
        setLocationToColor(2, BallColor.NONE);
        setLocationToColor(3, BallColor.NONE);
        if (mOnRedTeam) {
            mOnRedTeam = false;
            mTeamChangeButton.setBackgroundResource(R.drawable.blue_button);
            mTeamChangeButton.setText("Team Blue");
        } else {
            mOnRedTeam = true;
            mTeamChangeButton.setBackgroundResource(R.drawable.red_button);
            mTeamChangeButton.setText("Team Red");
        }
        // setTeamToRed(mOnRedTeam); // This call is optional. It will reset your GPS and sensor heading values.
    }

    /**
     * Sends a message to Arduino to perform a ball color test.
     */
    public void handlePerformBallTest(View view) {
        sendCommand("ATTACH 111111");
        sendCommand("p");
        sendCommand(home);
//        // DONE: COmment this out later
//        Toast.makeText(this,"MESSAGE SENT", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);
        int receivedNumber;
        receivedNumber = Integer.parseInt(receivedCommand);
        hundredth = receivedNumber / 100;
        tenth = receivedNumber / 10 - hundredth * 10;
        oneth = receivedNumber / 1 - tenth * 10 - hundredth * 100;

        loc1 = hundredth;
        loc2 = tenth;
        loc3 = oneth;

        if (hundredth == 7){
            setLocationToColor(1,BallColor.NONE);
        }else if(hundredth == 1){
//            mBall1.setText("Blue");
            setLocationToColor(1,BallColor.BLUE);
        }else if(hundredth == 2){
//            mBall1.setText("Red");
            setLocationToColor(1,BallColor.RED);
        }else if(hundredth == 3){
//            mBall1.setText("Yellow");
            setLocationToColor(1,BallColor.YELLOW);
        }else if(hundredth == 4){
//            mBall1.setText("Green");
            setLocationToColor(1,BallColor.GREEN);
        }else if(hundredth == 5){
//            mBall1.setText("Black");
            setLocationToColor(1,BallColor.BLACK);
        }else if(hundredth == 6){
//            mBall1.setText("White");
            setLocationToColor(1,BallColor.WHITE);
        }

        if (tenth == 7){
//            mBall2.setText("---");
            setLocationToColor(2,BallColor.NONE);
        }else if(tenth == 1){
//            mBall2.setText("Blue");
            setLocationToColor(2,BallColor.BLUE);
        }else if(tenth == 2){
//            mBall2.setText("Red");
            setLocationToColor(2,BallColor.RED);
        }else if(tenth == 3){
//            mBall2.setText("Yellow");
            setLocationToColor(2,BallColor.YELLOW);
        }else if(tenth == 4){
//            mBall2.setText("Green");
            setLocationToColor(2,BallColor.GREEN);
        }else if(tenth == 5){
//            mBall2.setText("Black");
            setLocationToColor(2,BallColor.BLACK);
        }else if(tenth == 6){
//            mBall2.setText("White");
            setLocationToColor(2,BallColor.WHITE);
        }

        if (oneth == 7){
//            mBall3.setText("---");
            setLocationToColor(3,BallColor.NONE);
        }else if(oneth == 1){
//            mBall3.setText("Blue");
            setLocationToColor(3,BallColor.BLUE);
        }else if(oneth == 2){
//            mBall3.setText("Red");
            setLocationToColor(3,BallColor.RED);
        }else if(oneth == 3){
//            mBall3.setText("Yellow");
            setLocationToColor(3,BallColor.YELLOW);
        }else if(oneth == 4){
//            mBall3.setText("Green");
            setLocationToColor(3,BallColor.GREEN);
        }else if(oneth == 5){
//            mBall3.setText("Black");
            setLocationToColor(3,BallColor.BLACK);
        }else if(oneth == 6){
//            mBall3.setText("White");
            setLocationToColor(3,BallColor.WHITE);
        }
//        // DONE: Comment this out later
//        Toast.makeText(this,"MESSAGE RECEIVED", Toast.LENGTH_SHORT).show();
    }

    AlertDialog alert;
    /**
     * Clicks to the red arrow image button that should show a dialog window.
     */
    public void handleDrivingStraight(View view) {
        Toast.makeText(this, "handleDrivingStraight", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(GolfBallDeliveryActivity.this);
        builder.setTitle("Driving Straight Calibration");
        View dialoglayout = getLayoutInflater().inflate(R.layout.driving_straight_dialog, (ViewGroup) getCurrentFocus());
        builder.setView(dialoglayout);
        final NumberPicker rightDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.right_pwm_number_picker);
        rightDutyCyclePicker.setMaxValue(255);
        rightDutyCyclePicker.setMinValue(0);
        rightDutyCyclePicker.setValue(mRightStraightPwmValue);
        rightDutyCyclePicker.setWrapSelectorWheel(false);
        final NumberPicker leftDutyCyclePicker = (NumberPicker) dialoglayout.findViewById(R.id.left_pwm_number_picker);
        leftDutyCyclePicker.setMaxValue(255);
        leftDutyCyclePicker.setMinValue(0);
        leftDutyCyclePicker.setValue(mLeftStraightPwmValue);
        leftDutyCyclePicker.setWrapSelectorWheel(false);
        Button doneButton = (Button) dialoglayout.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                alert.dismiss();
            }
        });
        final Button testStraightButton = (Button) dialoglayout.findViewById(R.id.test_straight_button);
        testStraightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLeftStraightPwmValue = leftDutyCyclePicker.getValue();
                mRightStraightPwmValue = rightDutyCyclePicker.getValue();
                Toast.makeText(GolfBallDeliveryActivity.this, "TODO: Implement the drive straight test", Toast.LENGTH_SHORT).show();
                sendWheelSpeed(mLeftStraightPwmValue,mRightStraightPwmValue);
                mCommandHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendWheelSpeed(0,0);
                    }
                },3000);
            }
        });
        alert = builder.create();
        alert.show();
    }

    /**
     * Test GPS point when going to the Far ball (assumes Blue Team heading to red ball).
     */
    public void handleFakeGpsF0(View view) {
//        onLocationChanged(165,50,NO_HEADING,null);
    }

    public void handleFakeGpsF1(View view) {
//        onLocationChanged(209,50,0,null);
    }

    public void handleFakeGpsF2(View view) {
//        onLocationChanged(231,50,135,null);
    }

    public void handleFakeGpsF3(View view) {
//        onLocationChanged(240,41,35,null);
    }

    public void handleFakeGpsH0(View view) {
//        onLocationChanged(165,0,-179.9,null);

    }

    public void handleFakeGpsH1(View view) {
//        onLocationChanged(11,0,179.9,null);

    }

    public void handleFakeGpsH2(View view) {
//        onLocationChanged(9,0,-170,null);

    }

    public void handleFakeGpsH3(View view) {
//        onLocationChanged(0,-9,-170,null);

    }

    public void handleSetOrigin(View view) {
        mFieldGps.setCurrentLocationAsOrigin();
    }

    public void handleSetXAxis(View view) {
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }

    public void handleZeroHeading(View view) {
        mFieldOrientation.setCurrentFieldHeading(0);
    }

    public void handleGoOrMissionComplete(View view) {
        if (mState == State.READY_FOR_MISSION) {
            // This is the moment in time, when the match starts!
            mMatchStartTime = System.currentTimeMillis();
//            updateMissionStrategyVariables();
            mGoOrMissionCompleteButton.setBackgroundResource(R.drawable.red_button);
            mGoOrMissionCompleteButton.setText("Mission Complete!");
            setState(State.LAB7);
        } else {
            setState(State.READY_FOR_MISSION);
        }
    }

    /** Updates the mission strategy variables. */
    private void updateMissionStrategyVariables() {
        mNearBallGpsY = -50.0; // Note, X value is a constant so no need to figure it out.
        mFarBallGpsY = 50.0; // Note, X value is a constant so no need to figure it out.
        mNearBallLocation = 1;
        mWhiteBallLocation = 0; // Assume there is no white ball present for now (update later).
        mFarBallLocation = 3;

        // Example of doing real planning.
        for (int i = 0; i < 3; i++) {
            BallColor currentLocationsBallColor = mLocationColors[i];
            if (currentLocationsBallColor == BallColor.WHITE) {
                mWhiteBallLocation = i + 1;
            }
            // TODO: In your project you’ll add more code to calculate the values below correctly!
        }

        Log.d(TAG, "Near ball position: " + mNearBallLocation + " drop off at y = " + mNearBallGpsY);
        Log.d(TAG, "Far ball position: " + mFarBallLocation + " drop off at y = " + mFarBallGpsY);
        Log.d(TAG, "White ball position: " + mWhiteBallLocation);
    }




}
