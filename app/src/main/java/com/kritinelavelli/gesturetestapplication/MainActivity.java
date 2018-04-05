package com.kritinelavelli.gesturetestapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    public String hand = "rightThumb";
    public static class point {
        public float x;
        public float y;
        public float pressure, orientation, size, touchMajor, touchMinor;
        public Long time;
        public point () {

        }
        public point (float a, float b, float pres, float orien, float si, float major, float minor, Long tim) {
            x = a;
            y = b;
            pressure = pres;
            orientation = orien;
            size = si;
            touchMajor = major;
            touchMinor = minor;
            time = tim;

        }
    }
    public static class swipe{
        public point start;
        public int width;
        public int height;
        public float xdpi;
        public List<point> coordinates;
        public String hand;
        public swipe() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }
    }
    swipe s;
    DisplayMetrics displayMetrics;

    FirebaseDatabase database;
    DatabaseReference myRef;
    int uniqueID;

    RadioGroup group;
    private View scrollView;
    private TextView result1;
    private TextView result2;
    //private TextView textView;
    private int numberOfPoints;
    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = findViewById(R.id.scroll);
        group = findViewById(R.id.group);
        result1 = findViewById(R.id.result1);
        result2 = findViewById(R.id.result2);
        //textView = findViewById(R.id.indicator);
        scrollView.setOnTouchListener(myOnTouchListener());

        Context context = getApplicationContext();
        CharSequence text = "Hello toast!";
        int duration = Toast.LENGTH_SHORT;
        toast = Toast.makeText(context, text, duration);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        myRef.child("uniqueID").addListenerForSingleValueEvent(postListener);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        numberOfPoints = 0;
    }

    ValueEventListener postListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            uniqueID = dataSnapshot.getValue(int.class) -1 ;
            //myRef.child("uniqueID").setValue(uniqueID+1);

            // ...
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            //Log.w("loadPost:onCancelled", databaseError.toException());
            // ...
        }
    };

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.leftThumb:
                if (checked)
                    hand = "leftThumb";
                break;
            case R.id.rightThumb:
                if (checked)
                    hand = "rightThumb";
                break;
            case R.id.leftIndex:
                if (checked)
                    hand = "leftIndex";
                break;
            case R.id.rightIndex:
                if (checked)
                    hand = "rightIndex";
                break;
        }
    }


    private View.OnTouchListener myOnTouchListener() {
        return  new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                view.onTouchEvent(event);
                Context context = getApplicationContext();
                numberOfPoints++;

//                        float x = event.getRawX();
//                        float y = event.getRawY();
                float x = view.getX()+event.getX();
                float y = view.getY()+event.getY();
                float pressure = event.getPressure();
                float orientation = event.getOrientation();
                float siz = event.getSize();
                float touchMajor = event.getTouchMajor();
                float touchMinor = event.getTouchMinor();

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        uniqueID+=1;
                        myRef.child("uniqueID").setValue(uniqueID);
                        //toast = Toast.makeText(context, "Down: "+numberOfPoints, Toast.LENGTH_SHORT);

//                                if (s!=null)
//                                    myRef.child("_"+uniqueID).setValue(s);
                        s = new swipe();
                        s.coordinates = new ArrayList<point>();
                        s.height = displayMetrics.heightPixels;
                        s.width = displayMetrics.widthPixels;
                        s.xdpi = displayMetrics.xdpi;
                        s.start = new point(x,y, pressure, orientation, siz, touchMajor, touchMinor, event.getEventTime());
                        s.hand = hand;

                        //toast.setText("Down");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //toast = Toast.makeText(context, "Move: "+numberOfPoints, Toast.LENGTH_SHORT);

                        int historySize = event.getHistorySize();
                        for (int h=0; h<historySize; h++) {
                            s.coordinates.add(new point(view.getX()+event.getHistoricalX(h),view.getY()+event.getHistoricalY(h), event.getHistoricalPressure(h), event.getHistoricalOrientation(h), event.getHistoricalSize(h), event.getHistoricalTouchMajor(h), event.getHistoricalTouchMinor(h), event.getHistoricalEventTime(h)));
                        }
                        s.coordinates.add(new point(x,y, pressure, orientation, siz, touchMajor, touchMinor, event.getEventTime()));
                        //toast.setText("Move");
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        //toast = Toast.makeText(context, "Up: "+numberOfPoints, Toast.LENGTH_SHORT);

                        s.coordinates.add(new point(x,y, pressure, orientation, siz, touchMajor, touchMinor, event.getEventTime()));

                        AsyncTaskRunner runner = new AsyncTaskRunner();
                        runner.execute(s);

                        //toast.setText("Up");
                        break;
                }

                //toast.show();
                return true;


            }
        };
    }

    private class AsyncTaskRunner extends AsyncTask<swipe, Void, List<String> > {

        @Override
        protected List<String> doInBackground(swipe... swipes) {
            swipe t = swipes[0];
            List<point> array = t.coordinates;
            array.add(0, t.start);
            int n = array.size();
            double xbar=0, ybar=0, xSQbar=0;
            for (int i = 0; i<n; i++) {
                xbar+=array.get(i).x;
                ybar+=array.get(i).y;
                xSQbar+= (array.get(i).x*array.get(i).x);
            }
            xbar=xbar/n;
            ybar=ybar/n;
            xSQbar=xSQbar/n;

            double SXX = 0,SXY = 0, SXX2 = 0, SX2X2 = 0, SX2Y = 0;
            for (int i=0; i<n; i++) {
                SXX+=((array.get(i).x-xbar)*(array.get(i).x-xbar));
                SXY+=((array.get(i).x-xbar)*(array.get(i).y-ybar));
                SXX2+=((array.get(i).x-xbar)*((array.get(i).x*array.get(i).x)-xSQbar));
                SX2X2 += (((array.get(i).x*array.get(i).x)-xSQbar)*((array.get(i).x*array.get(i).x)-xSQbar));
                SX2Y +=(((array.get(i).x*array.get(i).x) - xSQbar)*(array.get(i).y-ybar));
            }
            SXX = SXX/n;
            SXY = SXY/n;
            SXX2 = SXX2/n;
            SX2X2 = SX2X2/n;
            SX2Y = SX2Y/n;

            double B = ((SXY*SX2X2)-(SX2Y*SXX2))/((SXX*SX2X2)-(SXX2*SXX2));

            double C = ((SX2Y*SXX)-(SXY*SXX2))/((SXX*SX2X2)-(SXX2*SXX2));

            double A = ybar - (B*xbar) - (C*xSQbar);

            double m=0, p=0;
            for (int i=0; i<n; i++) {
                m+=((array.get(i).y-(A+(B*array.get(i).x)+(C*array.get(i).x*array.get(i).x)))*(array.get(i).y-(A+(B*array.get(i).x)+(C*array.get(i).x*array.get(i).x))));
                p+=((array.get(i).y-ybar)*(array.get(i).y-ybar));
            }
            double r = Math.sqrt(1-(m/p));

            myRef.child("_"+uniqueID).setValue(t);

            String result1 = "A = "+A+"B = "+B+"C = "+C+"r = "+r;
            String result2 = "xbar = "+xbar+"ybar = "+ybar+"xSQbar = "+xSQbar;

            List<String> results = new ArrayList<String>();
            results.add(result1);
            results.add(result2);
            return results;

        }
        @Override
        protected void onPostExecute(List<String> results) {
            result1.setText(results.get(0));
            result2.setText(results.get(1));
        }
    }
}
