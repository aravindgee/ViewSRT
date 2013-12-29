
package com.example.srtview;

import java.util.Date;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.example.srtview.SRTGestureListener;
import com.example.test3.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity extends Activity
{
	//Timers - one for displaying the subtitles, one for managing gestures.
	Timer tmrDisplaySubtitles, tmrGestures;
	
	//Textview to show the subtitles
	TextView tvSubtitleDisplay;
			
	//SparseArray that stores subtitle information in key-value pairs
	SparseArray<SRTData> saSRTData;
	
	//A handler to the UI to write text to the textView
	final Handler hdlrWriteText = new Handler();

	//Gesture listener for tapping & pausing
	private SRTGestureListener glGesture;
	GestureDetector gdGesture;
	
	//The number of the subtitle currently being displayed.
	int intCurrentSubtitleNumber;
	
	//Variable that notes the state of pause. Is matched with variable in SRTGestureListener class.
	boolean boolIsPaused;
	
	//String to store downloaded file contents
	String strSRTFileContents = new String();
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Initialize timer
		tmrDisplaySubtitles = new Timer();
		tmrGestures = new Timer();
		
		//Intialize Gestures
		glGesture = new SRTGestureListener();
		gdGesture = new GestureDetector(this, glGesture);
		
		boolIsPaused = false;
		
		intCurrentSubtitleNumber = 1;
		
		//Initialize SparseArray
		saSRTData = new SparseArray<SRTData>();
		
		//Get TextView where the text needs to be displayed.
		tvSubtitleDisplay = (TextView)findViewById(R.id.lblMsg);
	
		new Thread(new Runnable()
						{
			    			public void run()
			    			{
			    				try 
			    				{
			    					//Read file from server and store contents in a string
			    					HttpClient hcGetSRTFile = new DefaultHttpClient();
			    					HttpGet hgGetSRTFile = new HttpGet(getResources().getString(R.string.srt_url));
			    					HttpResponse hrGetSRTFile = hcGetSRTFile.execute(hgGetSRTFile); 
			    					HttpEntity heGetSRTFile = hrGetSRTFile.getEntity();
			    					strSRTFileContents = EntityUtils.toString(heGetSRTFile);
			    								    							
				    				//Read subtitle file and store the information in the SparseArray
				    				readSRTFile();
				    						
				    				//Schedule Timers
				    				tmrGestures.schedule(new GestureResponse(), 0);
				    				tmrDisplaySubtitles.schedule(new UpdateText(), 0);
			    				}
			    				catch(Exception e) { Log.e("ViewSRT", e.toString()); } //Write exception to logcat
			    			}
						}
					).start();
	}
	
	class SRTData //Class to store subtitle information in SparseArray
	{
		  public String subtitles;
		  public int time;
		  public SRTData(String subtitles, int time)
		  {
		    this.subtitles = subtitles;
		    this.time = time;
		  }
	}

	//Returns true if the given string is numeric
	public static boolean isNumeric(String str) 
	{
	  NumberFormat formatter = NumberFormat.getInstance();
	  ParsePosition pos = new ParsePosition(0);
	  formatter.parse(str, pos);
	  return str.length() == pos.getIndex();
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) //Establish gesture detection
	{
        if (!gdGesture.onTouchEvent(event))
            return super.onTouchEvent(event);
        return true;
    }
	
	class GestureResponse extends TimerTask
	{
		@Override
		public void run()
		{
			//Pause/unpause operation. 
			if(glGesture.boolIsPaused != boolIsPaused) 
    	    {
				if(glGesture.boolIsPaused)
				{
					tmrDisplaySubtitles.cancel();
	    	    	hdlrWriteText.post(new Runnable()
						{
		    				public void run() //Print PAUSE message on screen
		    				{
		    					tvSubtitleDisplay.setText("PAUSED");
		    				}
		    			}
	    	    	);
				}
				if(!glGesture.boolIsPaused)
				{
					tmrDisplaySubtitles = new Timer();
					tmrDisplaySubtitles.schedule(new UpdateText(), 0);
				}
					
				boolIsPaused = !boolIsPaused;
    	    }
			
			//Scroll or seek operation
			if(glGesture.intScrollDirection != 0)
			{
				tmrDisplaySubtitles.cancel();
				
				intCurrentSubtitleNumber = intCurrentSubtitleNumber + glGesture.intScrollDirection -1;
    		    glGesture.intScrollDirection = 0;
    		   
    		   //Make sure that the seeking does not exceed the boundaries
    		   if(intCurrentSubtitleNumber < 1)
    			   intCurrentSubtitleNumber = 1;
    		   if(intCurrentSubtitleNumber >= saSRTData.size())
    			   intCurrentSubtitleNumber = saSRTData.size();

    		   //Restart timer for displaying subtitles
    		   tmrDisplaySubtitles = new Timer();
    		   tmrDisplaySubtitles.schedule(new UpdateText(), 0);
    		   
    		   //Seeking will automatically unpause the display.
    		   boolIsPaused = glGesture.boolIsPaused = false;
			}
			tmrGestures.schedule(new GestureResponse(), 0);		
		}
	}
	
	class UpdateText extends TimerTask
	{		
      public void run()
      { 
    	  //Get object holding information of current subtitle
    	  final SRTData srtFromArray = saSRTData.get(intCurrentSubtitleNumber);
    	     	  
    	  hdlrWriteText.post(new Runnable()
			{
				public void run() //Print subtitle text on screen
				{					
					tvSubtitleDisplay.setText(Html.fromHtml(srtFromArray.subtitles));
				}
			}
    		);
   		  intCurrentSubtitleNumber++;
    	  tmrDisplaySubtitles.schedule(new UpdateText(), srtFromArray.time);
      }
	}
	
	public void readSRTFile() throws IOException
	{
		//Variables used in file reading.
		String strSubText = new String();
		String strSRTFileLine = new String();
		int intNextLineType = 1;
		int intSubtitleNumber = -1;
		SimpleDateFormat dfLineStart = new SimpleDateFormat("HH:mm:ss,S");
		SimpleDateFormat dfLineEnd = new SimpleDateFormat("HH:mm:ss,S");
		Pattern ptrnTimeLine = Pattern.compile("^(.*) --> (.{12})");
		Matcher mtchSubTime;
		Date dtStart = new Date();
		Date dtEnd = new Date();
		int intSubtitleDisplayTime = 0;
		
		//Open a stream to file contents
		Scanner scrSubFile = new Scanner(strSRTFileContents);
				
		//Read subtitles file line-by-line
		while(scrSubFile.hasNextLine())
		{
			strSRTFileLine = scrSubFile.nextLine();
			
			//If the line contains only a number (line type 1)
			if(isNumeric(strSRTFileLine) && intNextLineType == 1)
			{
				intNextLineType = 2;
				intSubtitleNumber = Integer.valueOf(strSRTFileLine);
				continue;
			}
			
			//If Line type is 2 (time)
			if(intNextLineType == 2)
			{
				mtchSubTime = ptrnTimeLine.matcher(strSRTFileLine);
				if (mtchSubTime.find()) //Get start time and end time of subtitles
				{
					try
					{
						dtStart = dfLineStart.parse(mtchSubTime.group(1)); 
						dtEnd = dfLineEnd.parse(mtchSubTime.group(2));
					}
					catch(Exception e){ Log.e("ViewSRT", e.toString()); }
					intSubtitleDisplayTime = (int) (dtEnd.getTime() - dtStart.getTime());
				}
				intNextLineType = 3;
				continue;
			}
			
			//If linetype is 3 (subtitles)
			else
			{
				if(!isNumeric(strSRTFileLine))
				{
				
					strSubText = strSubText.concat("<br>");
					strSubText = strSubText.concat(strSRTFileLine);
					continue;
				}
	
				saSRTData.put(intSubtitleNumber, new SRTData(strSubText, intSubtitleDisplayTime));
				intNextLineType = 1;
				strSubText = "";
			}
		}
		
		//Close the file
		scrSubFile.close();
		strSRTFileContents = "";
	}
}

