package com.pandian619.HighFrequencyDetector;


import ca.uol.aig.fftpack.RealDoubleFFT;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.TargetApi;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
/*
 *@author  Pandian
 *@version 1.0
 *@since   2014-09-06 
 *
 */
public class MainActivity extends Activity {
	private Button startRecButton,stopRecButton,clearTextButton;
	private TextView frequencyText,statusText;
	private String Tag="High Frequency Detector";
	private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private double[] magnitude;
	private boolean isRecording=false;  //variable to start or stop recording
	private int bufferSize;
	private short[] bufferByte;
	private short[] bufferDouble;
	private int peak;
	private AudioRecord audioRec;	
	private int blockSize,freq;
	private RealDoubleFFT transformer;
	private ProgressBar recordingProgress;
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		freq=Integer.parseInt(getString(R.string.SampleRate));
		blockSize=Integer.parseInt(getResources().getString(R.string.BufferSize));
		startRecButton=(Button)findViewById(R.id.startButton);
		stopRecButton=(Button)findViewById(R.id.stopButton);
		clearTextButton=(Button)findViewById(R.id.clearTextButton);
		frequencyText=(TextView)findViewById(R.id.freqText);
		statusText=(TextView)findViewById(R.id.statusText);
		recordingProgress=(ProgressBar)findViewById(R.id.progressBar1);
		recordingProgress.setVisibility(View.INVISIBLE);
		frequencyText.setMovementMethod(new ScrollingMovementMethod());
		int bufferSize = AudioRecord.getMinBufferSize(freq,channelConfiguration, audioEncoding);
		Log.i(Tag,"BUFFER SIZE="+bufferSize);
        if(AudioRecord.ERROR_BAD_VALUE == bufferSize){
               Log.e(Tag,"BUFFER ERROR");
               return;
          }
         else{	            	
           audioRec = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, freq,channelConfiguration, audioEncoding, bufferSize);	
          }
          handler=new Handler();
          startRecButton.setOnClickListener(new View.OnClickListener() {		
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				isRecording=true;
				statusText.setText("Listening...");
				if(android.os.Build.VERSION.SDK_INT>11 ){
					prograssAnimate();
				}
				
				recordingProgress.setVisibility(View.VISIBLE);
				RecordThread rt=new RecordThread();
				rt.start();
				}
          });
          clearTextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				frequencyText.setText("");
				}
          });
          stopRecButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.i("State",""+audioRec.getRecordingState());
				recordingProgress.setVisibility(View.INVISIBLE);
				statusText.setText("Listening Stops...");
				if(audioRec.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING){
					isRecording=false;
				}
			}
          });
		}	
	
	private void setFreqText(int PeakFreq){
		 Log.d("FREQ", "Peak Frequency = " + PeakFreq);
		   frequencyText.append("Max Frequency= "+PeakFreq+"\n");
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void prograssAnimate(){
		recordingProgress.animate();
	}

	@Override
	public void onDestroy() { 
	    super.onDestroy();
	    System.out.println("OnDestroy");
	}
	
	private class RecordThread extends Thread{   	
  	  	short[] buffer = new short[blockSize];
        double[] toTransform = new double[blockSize];
    
		private RecordThread(){
			magnitude=new double[blockSize];
			transformer= new RealDoubleFFT(blockSize);
			bufferByte=new short[bufferSize];
			bufferDouble=new short[bufferSize];
		}
		
		@Override
		public void run(){
			while(isRecording){				
				audioRec.startRecording();
				Log.i(Tag,"Recording Starred" );			
   			 	int bufferReadResult = audioRec.read(buffer, 0, blockSize);
                for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                    toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }
                transformer.ft(toTransform);
                double[] magnitude = new double[blockSize];
    			for(int j=0;j<(toTransform.length)/2;j++){
    			      magnitude[j] = Math.sqrt((toTransform[j]*toTransform[j]) + (toTransform[j+1]*toTransform[j+1]));
    			}
    			double max = 0.0;
    			int index = -1;
    			for(int j=0;j<(toTransform.length)/2;j++){    
    			    if(max < magnitude[j]){
    			            max = magnitude[j];
    			            index = j;
    			        }
    			 }
    			 peak = index * freq/blockSize;
    			 handler.post(new Runnable() {
    				 public void run() {
    					 setFreqText(peak);
    				 }
    			 });
			}
		}
	}
}


