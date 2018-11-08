package uk.ac.horizon.protestapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.ac.horizon.protestapp.MainActivity.ColorType.BLUE;
import static uk.ac.horizon.protestapp.MainActivity.ColorType.GREEN;
import static uk.ac.horizon.protestapp.MainActivity.ColorType.RED;

public class MainActivity extends AppCompatActivity
{
	private static final String ACTION_USB_PERMISSION =
			"com.android.example.USB_PERMISSION";
	UsbManager usbManager;
	TextView textView, instructionTextView, serialTextView;

	private CIELabColor color1 = null;
	private CIELabColor color2 = null;
	private CIELabColor color3 = null;

	private int[] color1f;
	private int[] color2f;
	private int[] color3f;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupPlayVideo(null);

	}

	@Override
	protected void onStart()
	{
		super.onStart();
		textView = (TextView) findViewById(R.id.textView);
		instructionTextView = (TextView) findViewById(R.id.instructionsTextView);
		serialTextView = (TextView) findViewById(R.id.serialTextView);
		usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(broadcastReceiver, filter);

		openArduinoSerial(null);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		closeArduinoSerial(null);
		unregisterReceiver(broadcastReceiver);
	}

	/* Functions exploring turning the device off. Does not work without root (not tested 
	   with root). */
	public void reboot(View v)
	{
		try
		{
			PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
			pm.reboot(null);
		}
		catch (Exception e)
		{

		}
	}

	public void actionRequestShutdown(View v)
	{
		try
		{
			Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
			intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		catch (Exception e)
		{

		}
	}

	public MediaPlayer mp;
	public SurfaceHolder holder;
	public SurfaceView videoSurface;
	public void setupPlayVideo(View v) {

		mp = new MediaPlayer();




		videoSurface = (SurfaceView) findViewById(R.id.videoView);


		holder = videoSurface.getHolder();

		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		holder.addCallback(new SurfaceHolder.Callback(){
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mp.setDisplay(holder);
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) { }
		});


	}

	public void playVideo1(View v)
	{
		tvAppend(textView, "Playing video 1.\n");
		playVideoFromDownloadDir("test1.mp4");
	}
	public void playVideo2(View v)
	{
		tvAppend(textView, "Playing video 2.\n");
		playVideoFromDownloadDir("test2.mp4");
	}
	public void playVideo3(View v)
	{
		tvAppend(textView, "Playing video 3.\n");
		playVideoFromDownloadDir("test3.mp4");
	}
	public void playVideoFromDownloadDir(String filename)
	{
		File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		File video = new File(downloadDir, filename);
		if (video.exists())
		{
			tvAppend(textView, "Video file found: "+video.getAbsolutePath()+"\n");
			Uri uri = Uri.parse(video.getAbsolutePath());
			playVideo(uri);
		}
		else
		{
			tvAppend(textView, "ERROR: Video does not exist: "+video.getAbsolutePath()+"\n");
		}
	}

	public void playVideo(int res)
	{
		playVideo(res, true);
	}
	public void playVideo(Uri uri)
	{
		playVideo(uri, true);
	}
	public void playVideo(int res, final boolean showBlackScreenAfter)
	{
		if (res == R.raw.blank)
		{
			tvAppend(textView, "Switching to blank screen.\n");
		}
		try
		{
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			if (mp != null)
			{
				mp.release();
			}
			mp = MediaPlayer.create(this, res);
			mp.setDisplay(holder);
			mp.start();
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
			{
				@Override
				public void onCompletion(MediaPlayer mp)
				{
					mp.stop();
					mp.release();
					if (showBlackScreenAfter)
					{
						// To clear the screen play a stored blank video.
						playVideo(R.raw.blank, false);
					}
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void playVideo(Uri uri, final boolean showBlackScreenAfter)
	{
		try
		{
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			if (mp != null)
			{
				mp.release();
			}
			mp = MediaPlayer.create(this, uri);
			mp.setDisplay(holder);
			mp.start();
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
			{
				@Override
				public void onCompletion(MediaPlayer mp)
				{
					mp.stop();
					mp.release();
					if (showBlackScreenAfter)
					{
						// To clear the screen play a stored blank video.
						playVideo(R.raw.blank, false);
					}
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mp!=null && mp.isPlaying())
		{
			mp.pause();
		}

	}

	@Override
	protected void onResume()
	{
		super.onResume();
		//mp.start();

		/*
		Handler h = new Handler(getMainLooper());

		h.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_LONG).show();
				playVideo1(null);
			}
		}, 1*1000);

		h.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_LONG).show();
				playVideo2(null);
			}
		}, 25*1000);

*/

	}

	/* Manual control using a keyboard. */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{

		switch (keyCode)
		{
			case KeyEvent.KEYCODE_1:
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						tvAppend(textView, "Keyboard input: 1\n");
						playVideo1(null);
					}
				});
				break;
			case KeyEvent.KEYCODE_2:
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						tvAppend(textView, "Keyboard input: 2\n");
						playVideo2(null);
					}
				});
				break;
			case KeyEvent.KEYCODE_3:
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						tvAppend(textView, "Keyboard input: 3\n");
						playVideo3(null);
					}
				});
				break;
			default:
				tvAppend(textView, "Keyboard input: other\n");
		}

		return super.onKeyDown(keyCode, event);
	}



	public boolean hasPerm = false;
	public UsbDevice device;
	public UsbDeviceConnection connection;
	public void openArduinoSerial(View view) {

		tvAppend(textView,"Attempting to open USB device:\n");

		HashMap<String,UsbDevice> usbDevices = usbManager.getDeviceList();
		if (!usbDevices.isEmpty()) {
			boolean keep = true;
			int count = 1;
			for (Map.Entry<String,UsbDevice> entry : usbDevices.entrySet()) {
				device = entry.getValue();
				int deviceVID = device.getVendorId();
				tvAppend(textView,"- " + count++ + " - ID: " + deviceVID + "\n");
				if (deviceVID == 0x2341 || deviceVID == 0x1A86)//Arduino Vendor ID
				{
					if (!hasPerm)
					{
						PendingIntent pi = PendingIntent.getBroadcast(this, 0,
								new Intent(ACTION_USB_PERMISSION), 0);
						usbManager.requestPermission(device, pi);
					}
					else
					{
						x();
					}
					keep = false;
				} else {
					connection = null;
					device = null;
				}

				if (!keep)
					break;
			}
			if (keep)
			{
				tvAppend(textView,"Arduino Vendor ID ("+0x2341+" or "+0x1A86+") not found.\n");
			}
		}
		else
		{
			tvAppend(textView,"- There are no devices.\n");
		}
	}

	public void closeArduinoSerial(View v)
	{
		try
		{
			serialPort.close();
			serialPort = null;
		}
		catch (Exception e)
		{

		}

	}

	private void x()
	{
			tvAppend(textView,"USB permission granted.\n");
			connection = usbManager.openDevice(device);
			serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
			if (serialPort != null) {
				if (serialPort.open()) { //Set Serial Connection Parameters.
					//setUiEnabled(true); //Enable Buttons in UI
					serialPort.setBaudRate(9600);
					serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
					serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
					serialPort.setParity(UsbSerialInterface.PARITY_NONE);
					serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
					serialPort.read(mCallback); //
					tvAppend(textView,"Serial Connection Opened! (16)\n");
					setInstructionText("Please present color 1.");

				} else {
					tvAppend(textView,"PORT NOT OPEN\n");
					Log.d("SERIAL", "PORT NOT OPEN");
				}
			} else {
				tvAppend(textView,"PORT IS NULL\n");
				Log.d("SERIAL", "PORT IS NULL");
			}
	}

	UsbSerialDevice serialPort;
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
				if (!hasPerm)
				{
					boolean granted =
							intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
					if (granted)
					{
						hasPerm = true;
						x();
					}
					else
					{
						tvAppend(textView, "PERM NOT GRANTED\n");
						Log.d("SERIAL", "PERM NOT GRANTED");
					}
				}
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
				tvAppend(textView,"USB device attached.\n");
				openArduinoSerial(null);
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
				tvAppend(textView,"USB device detached.\n");
				closeArduinoSerial(null);
			}
		};
	};

	UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
		//Defining a Callback which triggers whenever data is read.
		@Override
		public void onReceivedData(byte[] arg0) {
			String data = null;
			try {
				data = new String(arg0, "UTF-8");
				//data.concat("/n");
				tvAppend(serialTextView, data);
				parseSerialData(data);

			} catch (Exception e) {
				Log.e("SERIAL", "", e);
			}
		}
	};

	private Object appendLock = new Object();
	private void tvAppend(TextView tv, CharSequence text) {
		final TextView ftv = tv;
		final CharSequence ftext = text;
		runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				synchronized (appendLock)
				{
					String s = ftv.getText().toString() + ftext.toString();
					// limit to last 1000 characters
					ftv.setText(s.substring(s.length() < 500 ? 0 : s.length() - 500));
				}
			}
		});
	}

	private synchronized void parseSerialData(String data)
	{
		//tvAppend(textView, "parseSerialData ("+data+")\n");
		parseCommand(data);
	}

	private String join(String d, String[] a)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<a.length; ++i)
		{
			if (i!=0)
			{
				sb.append(d);
			}
			sb.append(a[i]);
		}
		return sb.toString();
	}

	private void parseCommand(String command)
	{
		//tvAppend(textView, "parseCommand("+command+")\n");
		command = command.trim();

		Pattern pattern = Pattern.compile(".*?W(-?\\d+).*");
		Matcher matcher = pattern.matcher(command);
		while (matcher.find()) {
			String weightCommand = matcher.group(1);
			try {
				int weight = Integer.parseInt(weightCommand);
				weightCommand(weight);
			} catch (Exception e) {
				tvAppend(textView,  "EXCEPTION running weight command: "+e.getMessage()+"\n");
			}
		}

		pattern = Pattern.compile(".*?R(\\d+).*");
		matcher = pattern.matcher(command);
		while (matcher.find()) {
			/*
			tvAppend(textView, "groups: "+matcher.groupCount()+"\n");
			for (int i=0; i<matcher.groupCount()+1; ++i)
			{
				tvAppend(textView, "group "+i+": "+matcher.group(i)+"\n");
			}
			*/
			String colorCommand = matcher.group(1);
			try {
				int red = Integer.parseInt(colorCommand);
				colorCommand(RED, red);
			} catch (Exception e) {
				tvAppend(textView,  "EXCEPTION running red command: "+e.getMessage()+"\n");
			}
		}
		pattern = Pattern.compile(".*?G(\\d+).*");
		matcher = pattern.matcher(command);
		while (matcher.find()) {
			String colorCommand = matcher.group(1);
			try {
				int green = Integer.parseInt(colorCommand);
				colorCommand(GREEN, green);
			} catch (Exception e) {
				tvAppend(textView,  "EXCEPTION running green command: "+e.getMessage()+"\n");
			}
		}
		pattern = Pattern.compile(".*?B(\\d+).*");
		matcher = pattern.matcher(command);
		while (matcher.find()) {
			String colorCommand = matcher.group(1);
			try {
				int blue = Integer.parseInt(colorCommand);
				colorCommand(BLUE, blue);
			} catch (Exception e) {
				tvAppend(textView,  "EXCEPTION running blue command: "+e.getMessage()+"\n");
			}
		}
	}

	private void weightCommand(int weight)
	{
		tvAppend(textView, "weightCommand("+weight+")\n");
	}

	public enum ColorType {RED, GREEN, BLUE}
	private int tempRed = -1, tempGreen = -1, tempBlue = -1;
	private synchronized void colorCommand(ColorType c, int value)
	{
		tvAppend(textView, "colorCommand("+c+", "+value+")\n");
		switch (c)
		{
			case RED:
				tempRed = value;
				break;
			case GREEN:
				tempGreen = value;
				break;
			case BLUE:
				tempBlue = value;
				break;
		}

		if (tempRed != -1 && tempGreen != -1 && tempBlue != -1)
		{
			//tvAppend(textView, "new RGBColor("+freq2byte(tempRed)+", "+freq2byte(tempGreen)+", "+freq2byte(tempBlue)+")");
			//colorCommand(new RGBColor(freq2byte(tempRed), freq2byte(tempGreen), freq2byte(tempBlue)));
			colorCommand(tempRed, tempGreen, tempBlue);
			tempRed = tempGreen = tempBlue = -1;
		}
	}
	private int freq2byte(double f) {
		return (int)((1024.0-f)/1024.0*255.0);
	}

	private void setInstructionText(final String text)
	{
		setInstructionText(text, Color.WHITE);
	}
	private void setInstructionText(final String text, final int color)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				instructionTextView.setTextColor(color);
				instructionTextView.setText(text);
			}
		});
	}

	private void colorCommand(int fr, int fg, int fb)
	{
		tvAppend(textView, "colorCommand(f"+fr+", f"+fg+", f"+fb+")\n");

		RGBColor rgbColor = new RGBColor(freq2byte(fr), freq2byte(fg), freq2byte(fb));
		CIELabColor labColor = rgbColor.toXYZColor().toCIELabColor();

		// record color if in setup
		if (color1 == null)
		{
			color1f = new int[] {fr, fg, fb};
			color1 = labColor;
			setInstructionText("Color 1 set. Please present color 2.", rgbColor.asInt());
			setVisibleDebug(true);
		}
		else if (color2 == null)
		{
			color2f = new int[] {fr, fg, fb};
			color2 = labColor;
			setInstructionText("Color 2 set. Please present color 3.", rgbColor.asInt());
			setVisibleDebug(true);
		}
		else if (color3 == null)
		{
			color3f = new int[] {fr, fg, fb};
			color3 = labColor;
			setInstructionText("Color 3 set. Ready for use.", rgbColor.asInt());


			Handler h = new Handler(getMainLooper());
			h.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					setVisibleDebug(false);
				}
			}, 1000);

		}
		else
		{
			// get difference between given color and recorded colors:
			double d1 = freqDiff(fr, fg, fb, color1f);
			double d2 = freqDiff(fr, fg, fb, color2f);
			double d3 = freqDiff(fr, fg, fb, color3f);

			tvAppend(textView, "Color differences: ["+d1+", "+d2+", "+d3+"]\n");

			// play video based on smallest difference:
			if (d1 < d2 && d1 < d3)
			{
				playVideo1(null);
			}
			else if (d2 < d3)
			{
				playVideo2(null);
			}
			else
			{
				playVideo3(null);
			}
		}
	}

	/** Get the absolute sum of difference between each sub-colour value. **/
	private int freqDiff(int fr, int fg, int fb, int[] color)
	{
		return Math.abs(fr-color[0]) + Math.abs(fg-color[1]) +Math.abs(fb-color[2]);
	}

	private void colorCommand(RGBColor color)
	{
		tvAppend(textView, "colorCommand("+color.r+", "+color.g+", "+color.b+")\n");

		CIELabColor labColor = color.toXYZColor().toCIELabColor();

		// record color if in setup
		if (color1 == null)
		{
			color1 = labColor;
			setInstructionText("Color 1 set. Please present color 2.", color.asInt());
			setVisibleDebug(true);
		}
		else if (color2 == null)
		{
			color2 = labColor;
			setInstructionText("Color 2 set. Please present color 3.", color.asInt());
			setVisibleDebug(true);
		}
		else if (color3 == null)
		{
			color3 = labColor;
			setInstructionText("Color 3 set. Ready for use.", color.asInt());


			Handler h = new Handler(getMainLooper());
			h.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					setVisibleDebug(false);
				}
			}, 1000);

		}
		else
		{
			// get difference between given color and recorded colors:
			double d1 = labColor.deltaE94(color1);
			double d2 = labColor.deltaE94(color2);
			double d3 = labColor.deltaE94(color3);

			tvAppend(textView, "Color differences: ["+d1+", "+d2+", "+d3+"]");

			// play video based on smallest difference:
			if (d1 < d2 && d1 < d3)
			{
				playVideo1(null);
			}
			else if (d2 < d3)
			{
				playVideo2(null);
			}
			else
			{
				playVideo3(null);
			}
		}
	}


	public void toggleDebug(View v)
	{
		setVisibleDebug(null);
	}
	public void setVisibleDebug(final Boolean b)
	{
		final View debugViewContainer = findViewById(R.id.debugViewContainer);
		if (debugViewContainer != null)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (b == null)
					{
						debugViewContainer.setVisibility(
								debugViewContainer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE
						);
					}
					else if (b)
					{
						debugViewContainer.setVisibility(View.VISIBLE);
					}
					else
					{
						debugViewContainer.setVisibility(View.INVISIBLE);
					}
				}
			});
		}
	}

	/* 
	   Unused colour translation functions.
	   Original idea was to translate detected colours in to the CIELab colour 
	   space/format and use the delta E94 algorithm to get a measure of how different 
	   two colours are in the context of the human visual system. However, the sensor 
	   does not produce standard RGB colours.
	 */

	public class RGBColor
	{
		private int clamp(int v, int min, int max)
		{
			return v < min ? min : (v > max ? max : v);
		}
		public int r, g, b;
		public RGBColor(int r, int g, int b)
		{
			this.r = clamp(r, 0, 255);
			this.g = clamp(g, 0, 255);
			this.b = clamp(b, 0, 255);
		}
		public int asInt()
		{
			return Color.rgb(this.r, this.g, this.b);
		}
		public XYZColor toXYZColor()
		{
			// Unused, implementation removed.
			return new XYZColor(0,0,0);
		}
	}
	public class XYZColor
	{
		public double x, y, z;
		public XYZColor(double x, double y, double z)
		{
			this.x=x;
			this.y=y;
			this.z=z;
		}
		public CIELabColor toCIELabColor()
		{
			// Unused, implementation removed.
			return new CIELabColor(0,0,0);
		}
	}
	public class CIELabColor
	{
		public double l, a, b;
		public CIELabColor(double l, double a, double b)
		{
			this.l=l;
			this.a=a;
			this.b=b;
		}
		public double deltaE94(CIELabColor other)
		{
			// Unused, implementation removed.
			return 0;
		}
	}


}
