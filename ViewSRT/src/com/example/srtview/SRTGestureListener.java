package com.example.srtview;

import android.util.Log;
import android.view.MotionEvent;

public class SRTGestureListener extends android.view.GestureDetector.SimpleOnGestureListener
{
	//State of PAUSE
	public boolean boolIsPaused = false;
	
	//Subtitle scroll/seek based on fling direction. +1 = Next subtitle, -1 = Previous subtitle. 0 = no scrolling
	public int intScrollDirection = 0;
			
    public boolean onDoubleTap(MotionEvent e)
    {
    	boolIsPaused = !boolIsPaused;
    	return true;
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {     
    	if(e1.getY() - e2.getY() > 0)
    		intScrollDirection = -1;
    	else
    		intScrollDirection = 1;
        return true;
    } 
}
