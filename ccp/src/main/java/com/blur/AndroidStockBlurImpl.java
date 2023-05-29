package com.blur;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class AndroidStockBlurImpl implements BlurImpl {
	private RenderScript mRenderScript;
	private ScriptIntrinsicBlur mBlurScript;
	private Allocation mBlurInput, mBlurOutput;

	@Override
	public boolean prepare(Context context, Bitmap buffer, float radius) {
		if (mRenderScript == null) {
			try {
				mRenderScript = RenderScript.create(context);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
				}
			} catch (RSRuntimeException e) {
				if (isDebug(context)) {
					throw e;
				} else {
					// In release mode, just ignore
					release();
					return false;
				}
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mBlurScript.setRadius(radius);
		}

		mBlurInput = Allocation.createFromBitmap(mRenderScript, buffer,
				Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());

		return true;
	}

	@Override
	public void release() {
		if (mBlurInput != null) {
			mBlurInput.destroy();
			mBlurInput = null;
		}
		if (mBlurOutput != null) {
			mBlurOutput.destroy();
			mBlurOutput = null;
		}
		if (mBlurScript != null) {
			mBlurScript.destroy();
			mBlurScript = null;
		}
		if (mRenderScript != null) {
			mRenderScript.destroy();
			mRenderScript = null;
		}
	}

	@Override
	public void blur(Bitmap input, Bitmap output) {
		mBlurInput.copyFrom(input);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mBlurScript.setInput(mBlurInput);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			mBlurScript.forEach(mBlurOutput);
		}
		mBlurOutput.copyTo(output);
	}

	// android:debuggable="true" in AndroidManifest.xml (auto set by build tool)
	static Boolean DEBUG = null;

	static boolean isDebug(Context ctx) {
		if (DEBUG == null && ctx != null) {
			DEBUG = (ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		}
		return DEBUG == Boolean.TRUE;
	}
}
