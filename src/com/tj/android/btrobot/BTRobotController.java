package com.tj.android.btrobot;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.anddev.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.anddev.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.anddev.andengine.engine.handler.physics.PhysicsHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.widget.Toast;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 *
 * @author Nicolas Gramlich
 * @since 00:06:23 - 11.07.2010
 */
public class BTRobotController extends BaseExample {
	// ===========================================================
	// Constants
	// ===========================================================

	private final static String TAG = "BTRobotCnt";
	
	private final static int LEFT_WHEEL = 2;
	private final static int RIGHT_WHEEL = 1;
	private final static int MAX_STEP = 10;
	
	private BTRobotCom mBTCom = null;	
	private String mBTMAC = null;
	
	private static final int CAMERA_WIDTH = 640;
	private static final int CAMERA_HEIGHT = 380;
	private static final int SW_CNT = 5;

	// ===========================================================
	// Fields
	// ===========================================================

	private Camera mCamera;
	
	private BitmapTextureAtlas mSwTextureAtlas[];
	private TextureRegion mSwTextureRegion[];
	private boolean mSwState[];
	private Sprite mSwSprite[];

	private BitmapTextureAtlas mOnScreenControlTexture;
	private TextureRegion mOnScreenControlBaseTextureRegion;
	private TextureRegion mOnScreenControlKnobTextureRegion;
	private RepeatingSpriteBackground mBackground;
	
	private float prevX = 0, prevY = 0;	
	private boolean mPlaceOnScreenControlsAtDifferentVerticalLocations = false;
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	//@Override
	public Engine onLoadEngine() {
		//Toast.makeText(this, "Also try tapping this AnalogOnScreenControl!", Toast.LENGTH_LONG).show();
		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		final Engine engine = new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, 
										new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera));

		try {
			if(MultiTouch.isSupported(this)) {
				engine.setTouchController(new MultiTouchController());
				if(MultiTouch.isSupportedDistinct(this)) {
					//Toast.makeText(this, "MultiTouch detected --> Both controls will work properly!", Toast.LENGTH_LONG).show();
				} else {
					this.mPlaceOnScreenControlsAtDifferentVerticalLocations = true;
					Toast.makeText(this, "MultiTouch detected, but your device has problems distinguishing between fingers.\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, "Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
			}
		} catch (final MultiTouchException e) {
			Toast.makeText(this, "Sorry your Android Version does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
		}
		return engine;
	}

	//@Override
	public void onLoadResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		
		// Switch Texture
		this.mSwTextureAtlas  = new BitmapTextureAtlas[SW_CNT];
		this.mSwTextureRegion = new TextureRegion[SW_CNT];
		this.mSwState = new boolean[SW_CNT];
		for (int i = 0; i < SW_CNT; i++) {
		    this.mSwTextureAtlas[i]  = new BitmapTextureAtlas(32, 32, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		    this.mSwTextureRegion[i] = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mSwTextureAtlas[i], this, "sw_off.png", 0, 0);
		    this.mSwState[i] = false;
		    this.mEngine.getTextureManager().loadTextures(this.mSwTextureAtlas[i]);
		}

		// Controller Texture
		this.mOnScreenControlTexture = new BitmapTextureAtlas(256, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, 
				this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, 
				this, "onscreen_control_knob.png", 128, 0);
		this.mEngine.getTextureManager().loadTextures(this.mOnScreenControlTexture);
		
		// Background Texture
		this.mBackground = new RepeatingSpriteBackground(CAMERA_WIDTH, CAMERA_HEIGHT, this.mEngine.getTextureManager(), 
				new AssetBitmapTextureAtlasSource(this, "gfx/background_grass.png"));
	}

	public int getTouchedSwIdx(final float x, final float y) {
		float tx, ty;
		
		for (int j = 0; j < SW_CNT; j++) {
			tx = mSwSprite[j].getX();
			ty = mSwSprite[j].getY();
			if (x >=  tx && x <= tx + this.mSwTextureRegion[j].getWidth() &&
				y >=  ty && y <= ty + this.mSwTextureRegion[j].getHeight())
					return j;
		}

		return -1;
	}
	
	//@Override
	public Scene onLoadScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.setBackground(this.mBackground);

		// Switch
		final int cx = (CAMERA_WIDTH - this.mSwTextureRegion[0].getWidth()) / 2 - 200;
		mSwSprite = new Sprite[SW_CNT];
		for (int i = 0; i < SW_CNT; i++) {
			int sx = cx + i * 100; //(this.mSwTextureRegion[i].getWidth() + 32);
			int sy = 20;
			mSwSprite[i] = new Sprite(sx, sy, this.mSwTextureRegion[i]) {
				@Override
				public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
					if(pSceneTouchEvent.isActionDown()) {
						int idx = getTouchedSwIdx(pSceneTouchEvent.getX(), pSceneTouchEvent.getY()); 
						Log.e(TAG, "TOUCH !!!" + idx);
						if (idx >= 0) {
							onToggleSw(idx);
							mSwSprite[idx].setScale(1.25f);
						}
					} else if (pSceneTouchEvent.isActionUp()) {
						int idx = getTouchedSwIdx(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
						if (idx >= 0) {
							mSwSprite[idx].setScale(1.0f);
						}
					}
					return true;
				}
			};
			scene.attachChild(mSwSprite[i]);
			scene.registerTouchArea(mSwSprite[i]);
		}
		
		// LEFT Analog
		final int x1 = 50;
		final int y1 = (CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight()) / 2 + 40;		
		final AnalogOnScreenControl leftAnalog = new AnalogOnScreenControl(x1, y1, 
		        this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, 200, 
		        new IAnalogOnScreenControlListener() {
			//@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
				onLeftAnalogMove(pValueX, pValueY);
			}

			//@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
			}
		});
		leftAnalog.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		leftAnalog.getControlBase().setAlpha(1.0f);
		leftAnalog.getControlBase().setScaleCenter(0, 128);
		leftAnalog.getControlBase().setScale(1.25f);
		leftAnalog.getControlKnob().setScale(1.25f);
		leftAnalog.refreshControlKnobPosition();
		scene.setChildScene(leftAnalog);

		// Right control
		final int x2 = CAMERA_WIDTH - this.mOnScreenControlBaseTextureRegion.getWidth() - 90;
		final int y2 = (CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight()) / 2 + 40;
		final AnalogOnScreenControl rightAnalog = new AnalogOnScreenControl(x2, y2, 
				this.mCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, 200,
				new IAnalogOnScreenControlListener() {
			@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
			}

			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
			}
		});
		rightAnalog.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		rightAnalog.getControlBase().setAlpha(1.0f);
		rightAnalog.getControlBase().setScaleCenter(0, 128);
		rightAnalog.getControlBase().setScale(1.25f);
		rightAnalog.getControlKnob().setScale(1.25f);
		rightAnalog.refreshControlKnobPosition();
		leftAnalog.setChildScene(rightAnalog);

		scene.setTouchAreaBindingEnabled(true);
		
		return scene;
	}
	
	//@Override
	public void onLoadComplete() {
        if (mBTCom == null)
            mBTCom = new BTRobotCom(this);
        
        Intent intent = getIntent();
	    mBTMAC = intent.getStringExtra("BT_MAC");
        
        BluetoothDevice device =  BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mBTMAC);
        // Attempt to connect to the device
        mBTCom.connect(device);
	}
	
    @Override
    public synchronized void onPause() {
        super.onPause();
        
        mBTCom.stop();
        mBTCom = null;
    }
   

	// ===========================================================
	// Methods
	// ===========================================================

    public void onToggleSw(int idx) {
		this.mSwTextureAtlas[idx].clearTextureAtlasSources();
		this.mSwState[idx] = !this.mSwState[idx];
		Log.e(TAG, "Toggle " + idx + ", val=" + this.mSwState[idx]);
		BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mSwTextureAtlas[idx], this, this.mSwState[idx] ? "sw_on.png" : "sw_off.png", 0, 0);
    }
    
    public void onLeftAnalogMove( final float pValueX, final float pValueY) {
		if (prevX != pValueX && prevY != pValueY)
		{
			float   scalar;
			int     left, right, diff;
			boolean	left_base = false;
			
			left  = 0;
			right = 0;
			diff  = 0;
			
			scalar = (float) Math.sqrt((float) (Math.pow(pValueX, 2) + Math.pow(pValueY, 2)));
//			physicsHandler.setVelocity(pValueX * 100, pValueY * 100);

			final float angleRad = MathUtils.atan2(pValueY, pValueX);
			final float angle    = MathUtils.radToDeg(angleRad) + 180;

			if (angle >= 0 && angle < 90)
			{
				diff      = MAX_STEP - (int) (angle / (90 / MAX_STEP));
				left_base = false; 
			}
			else if (angle >= 90 && angle < 180)
			{
				diff = ((int)angle - 90) / (90 / MAX_STEP);
				left_base = true;
			}
			else if (angle >= 180 && angle < 270)
			{
				diff   = MAX_STEP - ((int)angle - 180) / (90 / MAX_STEP);
				scalar = -scalar;
				left_base = true;
			}
			else if (angle >= 270 && angle < 360)
			{
				diff   = ((int)angle - 270) / (90 / MAX_STEP);
				scalar = -scalar;
				left_base = false;
			}
			
			diff = (int)(diff * scalar);
			
			if (left_base == true)
			{
				left  = (int)(scalar * MAX_STEP);
				right = left - diff;
			}
			else
			{
				right = (int)(scalar * MAX_STEP);
				left  = right - diff;
			}
			
			Log.e(TAG, "ANG:" +  angle + " left:" + left + " right:" + right + " diff:" + diff);

			mBTCom.setMotorValue(RIGHT_WHEEL, right);
			mBTCom.setMotorValue(LEFT_WHEEL,  left);

			prevX = pValueX;
			prevY = pValueY;
		}    	
    }
    
    
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
