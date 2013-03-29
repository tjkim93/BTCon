package com.tj.android.btrobot;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.anddev.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.anddev.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;

import android.hardware.SensorManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import org.anddev.andengine.entity.primitive.Line;

import com.tj.android.btrobot.R;

public class BTReceiverController extends BaseExample implements IAccelerometerListener {
	// ============================================================================================
	// CONSTANTS 
	// ============================================================================================
	private final static String TAG = "BTReceiverController";
	private static final int CAMERA_WIDTH = 640;
	private static final int CAMERA_HEIGHT = 380;
	private static final int SW_CNT  = 5;
	
	private static final int SW_EXT_IDX = 5;
	private static final int SW_TRIM_IDX = 5;
	private static final int SW_SENSOR_IDX = 6;
	private static final int SW_HOLD_IDX = 7;
	private static final int SW_TEXTURE_CNT = SW_CNT + 3; // including throttle hold / trim
	
	
	private static final int IDX_BEG = 0;
/*	
	private static final int IDX_ELE = 1;
	private static final int IDX_RUD = 2;
	private static final int IDX_THR = 3;
	private static final int IDX_AIL = 4;
*/	
	private static final int IDX_ELE = 2;
	private static final int IDX_RUD = 4;
	private static final int IDX_THR = 3;
	private static final int IDX_AIL = 1;
	
	
	private static final int IDX_SWI = 5;
	private static final int MAX_DATA_SIZE = 10;
	private static final float SCALE_FACTOR = 1.3f;
	
	private static final int ARROW_BTN_CNT   = 4;
	private static final int ARROW_UP_IDX    = 0;
	private static final int ARROW_DOWN_IDX  = 1;
	private static final int ARROW_LEFT_IDX  = 2;
	private static final int ARROW_RIGHT_IDX = 3;
	
	private static final int ARROW_BTN_SPRITE_CNT = 8;
	
	private static final int IDX_STICK_LEFT   = 0;
	private static final int IDX_STICK_RIGHT  = 1;
	private static final int IDX_INPUT_STICK  = 0;
	private static final int IDX_INPUT_TRIM   = 1;
	private static final int IDX_INPUT_SENSOR = 2;
	
	// ============================================================================================
	// VARIABLES 
	// ============================================================================================
	private BTReceiverCom m_btCom = null;	
	private String        m_strBTMac = null;	

	private Camera m_Camera;
	private Scene  m_Scene;
	private RepeatingSpriteBackground m_rsbBackGround;
	
	private BitmapTextureAtlas m_btaSwitches[];
	private TextureRegion      m_trSwitches[];
	private boolean            m_bSwitches[];
	private Sprite             m_sprSwitches[];
	private Text               m_textSwitches[];
	
	private BitmapTextureAtlas m_btaButtons[];
	private TextureRegion      m_trButtons[];
	private Sprite             m_sprButtons[];
	
	private BitmapTextureAtlas m_btaOSC;
	private TextureRegion      m_trOSC;
	private TextureRegion      m_trOSCKnob;
		
	private BitmapTextureAtlas m_btaFont;
	private Font               m_Font;

    private AnalogOnScreenControl m_aoscLeft;
    private AnalogOnScreenControl m_aoscRight;
    
    private float m_fPrevLX = -1;
    private float m_fPrevLY = -1;
    
    private float m_fPrevRX = -1;
    private float m_fPrevRY = -1;

	private int         m_nThrottleVal = 0;
	private int         m_nThrottleRange = 0;
	private int         m_nStickMode = 1;
	private boolean     m_bRevThrottle = false;
	private boolean     m_bRevAileron  = false;
	private boolean     m_bRevElevator = false;
	private boolean     m_bRevRudder   = false;
	private int         m_nTrimThrottle = 0;
	private int         m_nTrimAileron  = 127;
	private int         m_nTrimElevator = 127;
	private int         m_nTrimRudder   = 127;
	private int         m_nSensitivity = 0;

	private boolean     m_bOSCDiffVLoc = false;
	private byte        m_ucData2Send[];
	
    private Timer       m_tmrTxData;
    private RefreshTask m_tskTxData;
    private Handler     m_hndTxData = null;
   
    BTConApp app;
    
    
	// ============================================================================================
	// StickMarker Class
	// ============================================================================================
    class StickMarker {
    	private final int MARKER_WH = 10;
    	
    	private int       m_nX;
    	private int       m_nY;
    	private int       m_nW;
    	private int       m_nH;
    	
    	private int       m_nMX;
    	private int       m_nMY;
    	private Rectangle m_rectDot;
    	
    	StickMarker(int x, int y, int w, int h) {
    		m_nX = x;
    		m_nY = y;
    		m_nW = w;
    		m_nH = h;
    		m_nMX = (m_nX + m_nW / 2) - MARKER_WH / 2;
			m_nMY = (m_nY + m_nH / 2) - MARKER_WH / 2;
    	}
    	
    	void draw(Scene scene) {
			final Rectangle rect = new Rectangle(m_nX, m_nY, m_nW, m_nH);
			rect.setColor(0.3f, 0.3f, 0.3f);
			rect.setAlpha(0.5f);
			scene.attachChild(rect);
			
			final Line lineH = new Line(m_nX, m_nY + m_nH / 2, m_nX + m_nW, m_nY + m_nH / 2, 1);
			lineH.setColor(1.0f, 1.0f, 0);
			scene.attachChild(lineH);
			
			final Line lineV = new Line(m_nX + m_nW / 2, m_nY, m_nX + m_nW / 2, m_nY + m_nH, 1);
			lineV.setColor(1.0f, 1.0f, 0);
			scene.attachChild(lineV);

			m_rectDot = new Rectangle(m_nMX, m_nMY, MARKER_WH, MARKER_WH);
			m_rectDot.setColor(1.0f, 0, 0);
			m_rectDot.setAlpha(1.0f);
			scene.attachChild(m_rectDot);
    	}
    	
    	void moveMarker(byte x, byte y) {
    		int dx = x & 0xFF;
    		int dy = y & 0xFF;
    		float tx, ty;
    		
    		tx = (float)dx / 256.0f * m_nW;
    		ty = (float)dy / 256.0f * m_nH;
    		
    		m_nMX = (m_nX + (int)tx) - MARKER_WH / 2;
			m_nMY = (m_nY + (int)ty) - MARKER_WH / 2;
			m_rectDot.setPosition(m_nMX, m_nMY);
    	}
    };
    
    private StickMarker	m_smRight;
    private StickMarker	m_smLeft;
    

	// ============================================================================================
	// onLoadEngine
	// ============================================================================================
	//@Override
	public Engine onLoadEngine() {
		
		app =  (BTConApp)getApplication();
		m_nStickMode   = app.m_nStickMode + 1;
		m_bRevThrottle = app.m_bRevThrottle;
		m_bRevAileron  = app.m_bRevAileron;
		m_bRevRudder   = app.m_bRevRudder;
		m_bRevElevator = app.m_bRevElevator;
		
		m_nTrimThrottle = app.m_nTrimThrottle;
		m_nTrimAileron  = app.m_nTrimAileron;
		m_nTrimRudder   = app.m_nTrimRudder;
		m_nTrimElevator = app.m_nTrimElevator;
		
		m_nSensitivity  = app.m_nSensitivity;

		Log.e(TAG, "Stick " + m_nStickMode);
		
		//Toast.makeText(this, "Also try tapping this AnalogOnScreenControl!", Toast.LENGTH_LONG).show();
		this.m_Camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		final Engine engine = new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, 
										new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.m_Camera));

		try {
			if(MultiTouch.isSupported(this)) {
				engine.setTouchController(new MultiTouchController());
				if(MultiTouch.isSupportedDistinct(this)) {
					//Toast.makeText(this, "MultiTouch detected --> " +
					//		"Both controls will work properly!", Toast.LENGTH_LONG).show();
				} else {
					this.m_bOSCDiffVLoc = true;
					Toast.makeText(this, "MultiTouch detected, " + 
							"but your device has problems distinguishing between fingers.\n\n" +
							"Controls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, "Sorry your device does NOT support MultiTouch!\n\n" + 
						"(Falling back to SingleTouch.)\n\n" + 
						"Controls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
			}
		} catch (final MultiTouchException e) {
			Toast.makeText(this, "Sorry your Android Version does NOT support MultiTouch!\n\n" + 
					"(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
		}
		return engine;
	}

	// ============================================================================================
	// onLoadResources
	// ============================================================================================	
	//@Override
	public void onLoadResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		
		// Switch Texture
		this.m_btaSwitches  = new BitmapTextureAtlas[SW_TEXTURE_CNT];
		this.m_trSwitches = new TextureRegion[SW_TEXTURE_CNT];
		this.m_bSwitches = new boolean[SW_TEXTURE_CNT];
		for (int i = 0; i < SW_TEXTURE_CNT; i++) {
		    this.m_btaSwitches[i]  = new BitmapTextureAtlas(32, 32, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		    this.m_trSwitches[i] = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.m_btaSwitches[i], this, "sw_off.png", 0, 0);
		    this.m_bSwitches[i] = false;
		    this.mEngine.getTextureManager().loadTextures(this.m_btaSwitches[i]);
		}
		
		// Trim Button Texture
		this.m_btaButtons  = new BitmapTextureAtlas[ARROW_BTN_CNT];
		this.m_trButtons = new TextureRegion[ARROW_BTN_CNT];
		String strLabel[] = {
		    "up",
		    "down",
		    "left",
		    "right"
		};
		String filename;
		
		for (int i = 0; i < ARROW_BTN_CNT; i++) {
		    this.m_btaButtons[i]  = new BitmapTextureAtlas(32, 32, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		    filename = String.format("arrow_%s.png", strLabel[i]);
		    Log.e(TAG, filename);
		    this.m_trButtons[i] = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.m_btaButtons[i], this, filename, 0, 0);
		    this.mEngine.getTextureManager().loadTextures(this.m_btaButtons[i]);
		}
		
		// Controller Texture
		this.m_btaOSC = new BitmapTextureAtlas(256, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.m_trOSC = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.m_btaOSC, 
				this, "onscreen_control_base.png", 0, 0);
		this.m_trOSCKnob = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.m_btaOSC, 
				this, "onscreen_control_knob.png", 128, 0);
		this.mEngine.getTextureManager().loadTextures(this.m_btaOSC);


		// font
		this.m_btaFont = new BitmapTextureAtlas(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.m_Font = new Font(this.m_btaFont, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 16, true, Color.YELLOW);
		this.mEngine.getTextureManager().loadTexture(this.m_btaFont);
		this.getFontManager().loadFont(this.m_Font);		
		
		// Background Texture
		this.m_rsbBackGround = new RepeatingSpriteBackground(CAMERA_WIDTH, CAMERA_HEIGHT, this.mEngine.getTextureManager(),
				new AssetBitmapTextureAtlasSource(this, "gfx/background_grass.png"));
	}

	// Switches
	private void LoadSwitches(Scene scene) {
		
		class ExtraBtn {
			private int 	m_nX;
			private int		m_nY;
			private String  mLabel;
			
			ExtraBtn(int x, int y, String label) {
				m_nX     = x;
				m_nY     = y;
				mLabel = label;
			}
		};
		
		ExtraBtn extBtn[] = new ExtraBtn[3];
		extBtn[0] = new ExtraBtn(CAMERA_WIDTH / 2 - (this.m_trSwitches[0].getWidth() / 2) - 90, CAMERA_HEIGHT - 80, getString(R.string.btn_trim));
		extBtn[1] = new ExtraBtn(CAMERA_WIDTH / 2 - (this.m_trSwitches[0].getWidth() / 2) + 0, CAMERA_HEIGHT - 80, getString(R.string.btn_sensor));
		extBtn[2] = new ExtraBtn(CAMERA_WIDTH / 2 - (this.m_trSwitches[0].getWidth() / 2) + 90, CAMERA_HEIGHT - 80, getString(R.string.btn_hold));
		
		// Switch
		m_textSwitches = new Text[SW_TEXTURE_CNT];
		final int cx = (CAMERA_WIDTH - this.m_trSwitches[0].getWidth()) / 2 - 200;
		m_sprSwitches = new Sprite[SW_TEXTURE_CNT];
		for (int i = 0; i < SW_TEXTURE_CNT; i++) {
			int sx = cx + i * 100;
			int sy = 20;

			if (i >= SW_EXT_IDX) {
				sx = extBtn[i - SW_EXT_IDX].m_nX;
				sy = extBtn[i - SW_EXT_IDX].m_nY;
			}
			
			m_sprSwitches[i] = new Sprite(sx, sy, this.m_trSwitches[i]) {
				@Override
				public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
					BTReceiverController.this.runOnUpdateThread(new Runnable() {
						@Override
						public void run() {
							if(pSceneTouchEvent.isActionDown()) {
								int idx = getTouchedSwIdx(pSceneTouchEvent.getX(), pSceneTouchEvent.getY()); 
								Log.e(TAG, "TOUCH !!!" + idx);
								if (idx >= 0) {
									m_sprSwitches[idx].setScale(1.25f);
									updateToggleSW(idx);
								}
							} else if (pSceneTouchEvent.isActionUp()) {
								int idx = getTouchedSwIdx(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
								if (idx >= 0) {
									m_sprSwitches[idx].setScale(1.0f);
								}
							}
						}
					});
					
					return true;
				}
			};
			scene.attachChild(m_sprSwitches[i]);
			scene.registerTouchArea(m_sprSwitches[i]);
			
			String str = "Ch" + (5 + i);
			if (i >= SW_EXT_IDX) {
				str = extBtn[i - SW_EXT_IDX].mLabel;
				sx = sx + 16 - (str.length() * 8);
			}
			m_textSwitches[i] = new Text(sx, sy + this.m_trSwitches[0].getHeight(), this.m_Font, str, HorizontalAlign.CENTER);
			scene.attachChild(m_textSwitches[i]);
		}
	}
	
	// Analog Control
	private void LoadAnalogControl(Scene scene) {
		// LEFT Analog
		final int x1 = 40;
		final int y1 = (CAMERA_HEIGHT - this.m_trOSC.getHeight()) / 2 + 40;		
		
		// Right control
		final int x2 = CAMERA_WIDTH - this.m_trOSC.getWidth() - 80;
		final int y2 = (CAMERA_HEIGHT - this.m_trOSC.getHeight()) / 2 + 40;
		
		m_aoscLeft = new AnalogOnScreenControl(x1, y1, 
		        this.m_Camera, this.m_trOSC, this.m_trOSCKnob, 0.1f, 200, 
		        new IAnalogOnScreenControlListener() {
			//@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
				
				if (pValueX == m_fPrevLX && pValueY == m_fPrevLY)
					return;

				updateStickData(IDX_STICK_LEFT, IDX_INPUT_STICK, (float)(pValueX * 1.2), (float)(pValueY * 1.2));
				m_fPrevLX = pValueX;
				m_fPrevLY = pValueY;
			}

			//@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
			}
		});
		m_aoscLeft.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		m_aoscLeft.getControlBase().setAlpha(1.0f);
		m_aoscLeft.getControlBase().setScaleCenter(0, 128);
		m_aoscLeft.getControlBase().setScale(SCALE_FACTOR);
		m_aoscLeft.getControlKnob().setScale(SCALE_FACTOR);
		m_aoscLeft.refreshControlKnobPosition();
		scene.setChildScene(m_aoscLeft);
		
		m_aoscRight = new AnalogOnScreenControl(x2, y2, 
				this.m_Camera, this.m_trOSC, this.m_trOSCKnob, 0.1f, 200,
				new IAnalogOnScreenControlListener() {
			@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {

				if (pValueX == m_fPrevRX && pValueY == m_fPrevRY)
					return;
				
				updateStickData(IDX_STICK_RIGHT, IDX_INPUT_STICK, (float)(pValueX * 1.2), (float)(pValueY * 1.2));
				m_fPrevRX = pValueX;
				m_fPrevRY = pValueY;
			}

			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
			}
		});
		m_aoscRight.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		m_aoscRight.getControlBase().setAlpha(1.0f);
		m_aoscRight.getControlBase().setScaleCenter(0, 128);
		m_aoscRight.getControlBase().setScale(SCALE_FACTOR);
		m_aoscRight.getControlKnob().setScale(SCALE_FACTOR);
		m_aoscRight.refreshControlKnobPosition();
		m_aoscLeft.setChildScene(m_aoscRight);

		updateMode(m_nStickMode, scene);		
	}
	
	private void updateStickData(int idx, int input, float pValueX, float pValueY) {
		if (idx == 0) {
			switch (m_nStickMode) {
			case 1:
				if (this.m_bSwitches[SW_SENSOR_IDX] == true)
					break;
				
				updateRudder(pValueX);
				updateEle(-pValueY);
				m_smLeft.moveMarker(m_ucData2Send[IDX_RUD], (byte) (0xff - m_ucData2Send[IDX_ELE]));
				break;
				
			case 2:
				if (this.m_bSwitches[SW_SENSOR_IDX] == false)
					updateRudder(pValueX);
				
				updateThrottle(pValueY);
				m_smLeft.moveMarker(m_ucData2Send[IDX_RUD], (byte) (0xff - m_ucData2Send[IDX_THR]));
				break;
				
			case 3:
				if (this.m_bSwitches[SW_SENSOR_IDX] == true)
					break;
				
				updateAileron(pValueX);
				updateEle(-pValueY);
				m_smLeft.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_ELE]));
				break;
				
			case 4:
				if (this.m_bSwitches[SW_SENSOR_IDX] == false)
					updateAileron(pValueX);
				
				updateThrottle(pValueY);
				m_smLeft.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_THR]));
				break;
				
			default:
				break;
			}
		} else {
			switch (m_nStickMode) {
			case 1:
				if (this.m_bSwitches[SW_SENSOR_IDX] == false)
					updateAileron(pValueX);
				
				updateThrottle(pValueY);
				m_smRight.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_THR]));
				break;
				
			case 2:
				if (this.m_bSwitches[SW_SENSOR_IDX] == true)
					break;
				
				updateAileron(pValueX);
				updateEle(-pValueY);
				m_smRight.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_ELE]));
				break;
				
			case 3:
				if (this.m_bSwitches[SW_SENSOR_IDX] == false)
					updateRudder(pValueX);
				
				updateThrottle(pValueY);
				m_smRight.moveMarker(m_ucData2Send[IDX_RUD], (byte) (0xff - m_ucData2Send[IDX_THR]));
				break;
				
			case 4:
				if (this.m_bSwitches[SW_SENSOR_IDX] == true)
					break;
				
				updateRudder(pValueX);
				updateEle(-pValueY);
				m_smRight.moveMarker(m_ucData2Send[IDX_RUD], (byte) (0xff - m_ucData2Send[IDX_ELE]));
				break;
				
			default:
				break;
			}
		}
	}
	
	// Trims
	private void LoadTrims(Scene scene) {
		// LEFT Analog
		final int x1 = 40;
		final int y1 = (CAMERA_HEIGHT - this.m_trOSC.getHeight()) / 2 + 40;		
		
		// Right control
		final int x2 = CAMERA_WIDTH - this.m_trOSC.getWidth() - 80;
		final int y2 = (CAMERA_HEIGHT - this.m_trOSC.getHeight()) / 2 + 40;
		
		// Left Trim
		final int w = (int)(m_aoscLeft.getControlBase().getWidth() * SCALE_FACTOR);
		final int h = (int)(m_aoscLeft.getControlBase().getHeight());		
		
		int pos[][] =
		{ 
			{ x1 - 30    , y1 - 10},
			{ x1 - 30    , y1 + h / 2 + 10},
			{ x1 + 20    , y1 + h - 10},
	        { x1 + w - 50, y1 + h - 10},
	        
			{ x2 + w     , y2 - 10},
			{ x2 + w     , y2 + h / 2 + 10},
			{ x2 + 20    , y2 + h - 10},
	        { x2 + w - 50, y2 + h - 10},	        
		};
		
		if (m_sprButtons == null) {
			m_sprButtons = new Sprite[ARROW_BTN_SPRITE_CNT];
			for (int i = 0; i < ARROW_BTN_SPRITE_CNT; i++) {
				
				m_sprButtons[i] = new Sprite(pos[i][0], pos[i][1], this.m_trButtons[i % 4]) {
					@Override
					public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
						Log.e(TAG, pSceneTouchEvent.getX() + ", " + pSceneTouchEvent.getY());
						if(pSceneTouchEvent.isActionDown()) {
							int idx = getTouchedTrimIdx(pSceneTouchEvent.getX(), pSceneTouchEvent.getY()); 
							Log.e(TAG, "TOUCH !!!" + idx);
							if (idx >= 0) {
								updateTrimSW(idx);
								m_sprButtons[idx].setScale(1.25f);
							}
						} else if (pSceneTouchEvent.isActionUp()) {
							int idx = getTouchedTrimIdx(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
							if (idx >= 0) {
								m_sprButtons[idx].setScale(1.0f);
							}
						}
						return true;
					}
				};
			}
		}
		
		for (int i = 0; i < ARROW_BTN_SPRITE_CNT; i++) {
			scene.attachChild(m_sprButtons[i]);
			scene.registerTouchArea(m_sprButtons[i]);
		}
	}
	
	// UnloadTrims
	private void UnloadTrims(Scene scene) {
		if (m_sprButtons == null)
			return;
		
		for (int i = 0; i < ARROW_BTN_SPRITE_CNT; i++) {
			scene.unregisterTouchArea(m_sprButtons[i]);
			scene.detachChild(m_sprButtons[i]);
		}
	}

	// ============================================================================================
	// onLoadScene
	// ============================================================================================	
	//@Override
	public Scene onLoadScene() {
		//this.mEngine.registerUpdateHandler(new FPSLogger());

		m_Scene = new Scene();
		//m_Scene.setBackground(this.m_rsbBackGround);
		m_Scene.setBackground(new ColorBackground(0.0f, 0.062745f, 0.32941f));

		LoadSwitches(m_Scene);
		LoadAnalogControl(m_Scene);
		m_Scene.setTouchAreaBindingEnabled(true);
		
		return m_Scene;
	}
	
	// Label & Stick StickMarker
	public void updateMode(int mode, Scene scene) {
		final int lax = (int)m_aoscLeft.getControlBase().getX();
		final int lay = (int)m_aoscLeft.getControlBase().getY();
		final int rax = (int)m_aoscRight.getControlBase().getX();
		final int ray = (int)m_aoscRight.getControlBase().getY();
		
		final int w = (int)(m_aoscRight.getControlBase().getWidth() * SCALE_FACTOR);
		final int h = (int)(m_aoscRight.getControlBase().getHeight());
		//final int h = (int)(m_aoscRight.getControlBase().getHeight() * 1.05f);
		
		int posThr[][] =
			{ 
				{ 0, 0 },
				{ rax + (w / 2) - 15, ray + h },
				{ lax + (w / 2) - 15, lay + h },
				{ rax + (w / 2) - 15, ray + h },
	            { lax + (w / 2) - 15, lay + h },
			};
		
		int posAil[][] =
			{ 
				{ 0, 0 },
	            { rax + w, ray + (h / 2) - 30 },
	            { rax + w, ray + (h / 2) - 30 },
	            { lax - 30, lay + (h / 2) - 30 },
	            { lax - 30, lay + (h / 2) - 30 },
			};

		int posEle[][] =
			{ 
				{ 0, 0 },
				{ lax + (w / 2) - 15, lay + h },
				{ rax + (w / 2) - 15, ray + h },
				{ lax + (w / 2) - 15, lay + h },
	            { rax + (w / 2) - 15, ray + h },
			};
		
		int posRud[][] =
			{ 
				{ 0, 0 },
	            { lax - 30, lay + (h / 2) - 30 },
	            { lax - 30, lay + (h / 2) - 30 },
	            { rax + w, ray + (h / 2) - 30 },
	            { rax + w, ray + (h / 2) - 30 },
			};
		
		final int rectWH = 100;
		m_nThrottleRange = rectWH;
		
		m_smLeft  = new StickMarker(lax + w, lay, rectWH, rectWH);
		m_smRight = new StickMarker(rax - rectWH, lay, rectWH, rectWH);
		m_smLeft.draw(scene);
		m_smRight.draw(scene);
		
		final Text txtThr = new Text(posThr[mode][0], posThr[mode][1], this.m_Font, "THR", HorizontalAlign.CENTER);
		scene.attachChild(txtThr);
		
		final Text txtAil = new Text(posAil[mode][0], posAil[mode][1], this.m_Font, "AIL", HorizontalAlign.CENTER);
		scene.attachChild(txtAil);
		
		final Text txtEle = new Text(posEle[mode][0], posEle[mode][1], this.m_Font, "ELE", HorizontalAlign.CENTER);
		scene.attachChild(txtEle);
		
		final Text txtRud = new Text(posRud[mode][0], posRud[mode][1], this.m_Font, "RUD", HorizontalAlign.CENTER);
		scene.attachChild(txtRud);
	}
	

	// ============================================================================================
	// onLoadComplete
	// ============================================================================================
	//@Override
	public void onLoadComplete() {
        if (m_btCom == null)
            m_btCom = new BTReceiverCom(this);
        
        Intent intent = getIntent();
	    m_strBTMac = intent.getStringExtra("BT_MAC");
        
        BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(m_strBTMac);
        // Attempt to connect to the device
        m_btCom.connect(device);
        
        m_ucData2Send = new byte[MAX_DATA_SIZE];
        m_ucData2Send[IDX_BEG] = 0;
        m_ucData2Send[IDX_ELE] = (byte)m_nTrimElevator;
        m_ucData2Send[IDX_RUD] = (byte)m_nTrimRudder;
        m_ucData2Send[IDX_THR] = (byte)m_nTrimThrottle;
        m_ucData2Send[IDX_AIL] = (byte)m_nTrimAileron;
        for (int i = IDX_SWI; i < MAX_DATA_SIZE; i++)
        	m_ucData2Send[i] = 0x01;
        
        startTimer();
	}

	
	// ============================================================================================
	// onResume
	// ============================================================================================	
	@Override
	public void onResumeGame() {
		super.onResumeGame();
	}

	
	// ============================================================================================
	// onPause
	// ============================================================================================
	public void saveTrim() {
		app.m_nTrimThrottle = m_nTrimThrottle;
		app.m_nTrimAileron  = m_nTrimAileron;
		app.m_nTrimRudder   = m_nTrimRudder;
		app.m_nTrimElevator = m_nTrimElevator;
		app.SaveSettings();
	}
	
	@Override
	public void onPauseGame() {
		super.onPauseGame();
		this.disableAccelerometerSensor();
       stopTimer();
        
        m_ucData2Send[IDX_BEG] = 0;
        m_ucData2Send[IDX_ELE] = 0x7f;
        m_ucData2Send[IDX_RUD] = 0x7f;
        m_ucData2Send[IDX_THR] = 0x01;
        m_ucData2Send[IDX_AIL] = 0x7f;
        for (int i = IDX_SWI; i < MAX_DATA_SIZE; i++)
        	m_ucData2Send[i] = 0x01;
        m_btCom.write(m_ucData2Send);

        m_btCom.stop();
        m_btCom = null;
        
        saveTrim();		
	}
	
	// ============================================================================================
	// THROTTLE
	// ============================================================================================
	public void updateThrottle(float pValue) {
		float val;
		float scaler = (float) Math.abs(pValue) * 5;
		
		if (m_bSwitches[SW_HOLD_IDX] == true)
			return;
		
		if (m_bRevThrottle == true)
			scaler = -scaler;
		
		// calc throttle
		if (pValue < 0)
			m_nThrottleVal += scaler;
		else if (pValue > 0)
			m_nThrottleVal -= scaler;
		
		if (m_nThrottleVal > m_nThrottleRange)
			m_nThrottleVal = m_nThrottleRange;
		else if (m_nThrottleVal < 0)
			m_nThrottleVal = 0;

		if (m_ucData2Send != null) {
			val = (float)m_nThrottleVal / (float)m_nThrottleRange * 255;
			val = Math.max(1, val);
			val = Math.min(255, m_nTrimThrottle + val);
			m_ucData2Send[IDX_THR] = (byte) val;
		}
	}
	
	// ============================================================================================
	// AILERON
	// ============================================================================================
	public void updateAileron(float pValue) {
		float val;

		if (m_bRevAileron == true)
			pValue = -pValue;
		
		// calc aileron
		float aileron = pValue * 128;
		val = (int) (m_nTrimAileron + aileron);
		if (val < 1)
			val = 1;
		else if (val > 255)
			val = 255;
		
		if (m_ucData2Send != null) {
			m_ucData2Send[IDX_AIL] = (byte) val;
		}
	}

	// ============================================================================================
	// Elevator
	// ============================================================================================
	public void updateEle(float pValue) {
		float val;

		if (m_bRevElevator == true)
			pValue = -pValue;
		
		// calc ele
		float yaw = pValue * 128;
		val = (int) (m_nTrimElevator + yaw);
		if (val < 1)
			val = 1;
		else if (val > 255)
			val = 255;
		
		if (m_ucData2Send != null) {
			m_ucData2Send[IDX_ELE] = (byte) val;
		}
	}

	// ============================================================================================
	// RUDDER
	// ============================================================================================
	public void updateRudder(float pValue) {
		float val;

		if (m_bRevRudder == true)
			pValue = -pValue;
		
		// calc rudder
		float rudder = pValue * 128;
		val = (int) (m_nTrimRudder + rudder);
		if (val < 1)
			val = 1;
		else if (val > 255)
			val = 255;
		
		if (m_ucData2Send != null) {
			m_ucData2Send[IDX_RUD] = (byte) val;
		}
	}
	
	// ============================================================================================
	// TOGGLE SW
	// ============================================================================================
	public int getTouchedSwIdx(final float x, final float y) {
		float tx, ty;
		
		for (int j = 0; j < SW_TEXTURE_CNT; j++) {
			tx = m_sprSwitches[j].getX();
			ty = m_sprSwitches[j].getY();
			if (x >=  tx && x <= tx + this.m_trSwitches[j].getWidth() &&
				y >=  ty && y <= ty + this.m_trSwitches[j].getHeight())
					return j;
		}

		return -1;
	}
	
	// updateToggleSW
    public void updateToggleSW(int idx) {
		this.m_btaSwitches[idx].clearTextureAtlasSources();
		this.m_bSwitches[idx] = !this.m_bSwitches[idx];
		Log.e(TAG, "Toggle " + idx + ", val=" + this.m_bSwitches[idx]);
		BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.m_btaSwitches[idx], this, this.m_bSwitches[idx] ? "sw_on.png" : "sw_off.png", 0, 0);
		if (m_ucData2Send != null && idx < SW_EXT_IDX ) {
			m_ucData2Send[IDX_SWI + idx] = (byte) ((this.m_bSwitches[idx] == true) ? 0xff : 0x01);
		}
		
		if (idx == SW_TRIM_IDX && this.m_bSwitches[SW_TRIM_IDX] == true) {
			LoadTrims(m_Scene);
		} else if (idx == SW_TRIM_IDX && this.m_bSwitches[SW_TRIM_IDX] == false) {
			UnloadTrims(m_Scene);
		}
		
		if (idx == SW_SENSOR_IDX && this.m_bSwitches[SW_SENSOR_IDX] == true) {
			this.enableAccelerometerSensor(this);
		} else if (idx == SW_SENSOR_IDX && this.m_bSwitches[SW_SENSOR_IDX] == false) {
			this.disableAccelerometerSensor();
			m_fPrevLX = -2;
			m_fPrevLY = -2;
			m_fPrevRX = -2;
			m_fPrevRY = -2;
		}
    }

	// ============================================================================================
	// TRIM SW
	// ============================================================================================
	public int getTouchedTrimIdx(final float x, final float y) {
		float tx, ty;
		
		for (int j = 0; j < ARROW_BTN_SPRITE_CNT; j++) {
			tx = m_sprButtons[j].getX();
			ty = m_sprButtons[j].getY();
			if (x >=  tx && x <= tx + this.m_trButtons[j % 4].getWidth() &&
				y >=  ty && y <= ty + this.m_trButtons[j % 4].getHeight())
					return j;
		}

		return -1;
	}

	private int Cut(int val, int min, int max) {
		int temp;
		
		temp = Math.max(val, min);
		temp = Math.min(temp, max);
		return temp;
	}
	
	// updateTrimSW
    public void updateTrimSW(int idx) {
		float val;
		
    	if (idx < 4) {
    		switch (m_nStickMode) {
    		case 1:
    			switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimElevator = Cut(++m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimElevator = Cut(--m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimRudder = Cut(--m_nTrimRudder, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimRudder = Cut(++m_nTrimRudder, 0, 255);
    				break;
    			}
    			updateStickData(IDX_STICK_LEFT, IDX_INPUT_TRIM, 0, 0);
    			break;
    			
    		case 2:
    			switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimThrottle = Cut(++m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimThrottle = Cut(--m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimRudder = Cut(--m_nTrimRudder, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimRudder = Cut(++m_nTrimRudder, 0, 255);
    				break;
    			}
    			updateStickData(IDX_STICK_LEFT, IDX_INPUT_TRIM, 0, 0);
    			break;
    			
    		case 3:
    			switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimElevator = Cut(++m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimElevator = Cut(--m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimAileron = Cut(--m_nTrimAileron, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimAileron = Cut(++m_nTrimAileron, 0, 255);
    				break;
    			}
    			updateStickData(IDX_STICK_LEFT, IDX_INPUT_TRIM, 0, 0);
    			break;
    			
    		case 4:
    			switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimThrottle = Cut(++m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimThrottle = Cut(--m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimAileron = Cut(--m_nTrimAileron, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimAileron = Cut(++m_nTrimAileron, 0, 255);
    				break;
    			}
    			updateStickData(IDX_STICK_LEFT, IDX_INPUT_TRIM, 0, 0);
    			break;
    			
    		default:
    			break;
    		}
    	} else {
    		idx -= 4;
    		switch (m_nStickMode) {
			case 1:
				switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimThrottle = Cut(++m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimThrottle = Cut(--m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimAileron = Cut(--m_nTrimAileron, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimAileron = Cut(++m_nTrimAileron, 0, 255);
    				break;
    			}
				updateStickData(IDX_STICK_RIGHT, IDX_INPUT_TRIM, 0, 0);
				break;
				
			case 2:
				switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimThrottle = Cut(++m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimThrottle = Cut(--m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimAileron = Cut(--m_nTrimAileron, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimAileron = Cut(++m_nTrimAileron, 0, 255);
    				break;
    			}
				updateStickData(IDX_STICK_RIGHT, IDX_INPUT_TRIM, 0, 0);
				break;
				
			case 3:
				switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimThrottle = Cut(++m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimThrottle = Cut(--m_nTrimThrottle, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimRudder = Cut(--m_nTrimRudder, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimRudder = Cut(++m_nTrimRudder, 0, 255);
    				break;
    			}
				updateStickData(IDX_STICK_RIGHT, IDX_INPUT_TRIM, 0, 0);
				break;
				
			case 4:
				switch (idx) {
    			case ARROW_UP_IDX:
    				m_nTrimThrottle = Cut(++m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_DOWN_IDX:
    				m_nTrimThrottle = Cut(--m_nTrimElevator, 0, 255);
    				break;
    			case ARROW_LEFT_IDX:
    				m_nTrimRudder = Cut(--m_nTrimRudder, 0, 255);
    				break;
    			case ARROW_RIGHT_IDX:
    				m_nTrimRudder = Cut(++m_nTrimRudder, 0, 255);
    				break;
    			}
				updateStickData(IDX_STICK_RIGHT, IDX_INPUT_TRIM, 0, 0);
				break;
				
			default:
				break;
			}
    	}
    }
    
	// ============================================================================================
	// TIMER
	// ============================================================================================
    class RefreshTask extends TimerTask {
        public void run() {
            m_hndTxData.sendEmptyMessage(0);
        }
    }

    // startTimer
    private void startTimer() {
        Log.e(TAG, "startTimer");

		m_hndTxData = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            super.handleMessage(msg);
	            switch (msg.what) {
	            case 0:
	            	
	            	if (m_btCom == null || !m_btCom.isConnected())
	            		break;
	            	
	            	String str = "";
	            	m_ucData2Send[IDX_BEG] = 0;
	            	
	            	for (int i = 0; i < MAX_DATA_SIZE; i++) {
	            		str = str + String.format("[%d] %02X ", i, m_ucData2Send[i]);
	            	}
	            	//Log.d(TAG, str);
	            	
	            	if (m_btCom != null && m_btCom.isConnected())
	            		m_btCom.write(m_ucData2Send);
	                break;
	            }
	        }
	    };
        
        m_tmrTxData = new Timer();
        m_tskTxData = new RefreshTask();
        m_tmrTxData.schedule(m_tskTxData, 0, 20);
    }

    // stopTimer
    private void stopTimer() {
        Log.e(TAG, "stopTimer");
        if (m_tmrTxData != null) {
            m_tmrTxData.cancel();
            //m_tmrTxData.purge();
            m_tmrTxData = null;
        }
        m_tskTxData = null;
    }

    
	// ============================================================================================
	// Accelerometer
	// ============================================================================================
	public void onAccelerometerChanged(final AccelerometerData pAccelerometerData) {
		String msg;
		float x = pAccelerometerData.getX() / (16 - m_nSensitivity);
		float y = pAccelerometerData.getY() / (16 - m_nSensitivity);
		
		msg = String.format("%f, %f", x, y);
		//Log.e(TAG, msg);

		switch (m_nStickMode) {
		case 1:
			updateAileron(x);
			updateEle(-y);
			m_smLeft.moveMarker(m_ucData2Send[IDX_RUD], (byte) (0xff - m_ucData2Send[IDX_ELE]));
			m_smRight.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_THR]));
			break;
		case 2:
			updateAileron(x);
			updateEle(-y);
			m_smRight.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_ELE]));
			break;
		case 3:
			updateAileron(x);
			updateEle(-y);
			m_smLeft.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_ELE]));
			break;
		case 4:
			updateAileron(x);
			m_smLeft.moveMarker(m_ucData2Send[IDX_AIL], (byte) (0xff - m_ucData2Send[IDX_THR]));
			updateEle(-y);
			m_smRight.moveMarker(m_ucData2Send[IDX_RUD], (byte) (0xff - m_ucData2Send[IDX_ELE]));			
			break;
		}
	}
}
