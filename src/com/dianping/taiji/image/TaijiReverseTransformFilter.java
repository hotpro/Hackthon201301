package com.dianping.taiji.image;

import android.graphics.Rect;
import android.util.Log;

import com.jabistudio.androidjhlabs.filter.TransformFilter;
import com.jabistudio.androidjhlabs.filter.math.ImageMath;
import com.jabistudio.androidjhlabs.filter.util.PixelUtils;

public class TaijiReverseTransformFilter extends TransformFilter {

	@Override
	protected void transformInverse(int x, int y, float[] out) {

	}
	
	protected void transformInverse(int x, int y, float[] out, int width,
			int height, float factor) {
		float cycle = (float) Math.min(width, height);
		float centerX = width / 2.0f;
		float centerY = height / 2.0f;
		float xCentered = x - centerX;
		float yCentered = y - centerY;
		double radius = Math.sqrt((double) (xCentered * xCentered + yCentered
				* yCentered));
		if (radius == 0) {
			return;
		}
		if (radius > centerX || radius > centerY) {
			out[0] = x;
			out[1] = y;
			return;
		}
		transformInverseCentered(xCentered, yCentered, out, radius, cycle,
				factor);
		out[0] += centerX;
		out[1] += centerY;
	}

	protected void transformInverseCentered(float x, float y, float[] out,
			double radius, float cycle, float factor) {
		double offset = Math.sin(radius / cycle * Math.PI * 2) * factor / 200
				* cycle;
		double angleOld = Math.acos(x / radius);
		if (y < 0) {
			angleOld = 2 * Math.PI - angleOld;
		}
		double angleNew = angleOld - offset * 1.0 / radius;

		out[0] = (float) (Math.cos(angleNew) * radius);
		out[1] = (float) (Math.sin(angleNew) * radius);}
	
	
	public int[] filter( int[] src ,int w, int h,int factor) {
        int width = w;
        int height = h;
        
        Log.d("DEBUG","width = "+width+"  height = "+height);
        
		originalSpace = new Rect(0, 0, width, height);
		transformedSpace = new Rect(0, 0, width, height);
		transformSpace(transformedSpace);
		int[] inPixels = src;
		int[] dst = new int[width * height];
		
		if ( interpolation == NEAREST_NEIGHBOUR )
			return filterPixelsNN( dst, width, height, inPixels, transformedSpace );

		int srcWidth = width;
		int srcHeight = height;
		int srcWidth1 = width-1;
		int srcHeight1 = height-1;
		int outWidth = transformedSpace.right;
		int outHeight = transformedSpace.bottom;
		int outX, outY;
		int[] outPixels = new int[outWidth];

		outX = transformedSpace.left;
		outY = transformedSpace.top;
		float[] out = new float[2];
		for (int y = 0; y < outHeight; y++) {
			for (int x = 0; x < outWidth; x++) {
				transformInverse(outX+x, outY+y, out,width,height,factor);
				int srcX = (int)Math.floor( out[0] );
				int srcY = (int)Math.floor( out[1] );
				float xWeight = out[0]-srcX;
				float yWeight = out[1]-srcY;
				int nw, ne, sw, se;

				if ( srcX >= 0 && srcX < srcWidth1 && srcY >= 0 && srcY < srcHeight1) {
					// Easy case, all corners are in the image
					int i = srcWidth*srcY + srcX;
					nw = inPixels[i];
					ne = inPixels[i+1];
					sw = inPixels[i+srcWidth];
					se = inPixels[i+srcWidth+1];
				} else {
					// Some of the corners are off the image
					nw = getPixel( inPixels, srcX, srcY, srcWidth, srcHeight );
					ne = getPixel( inPixels, srcX+1, srcY, srcWidth, srcHeight );
					sw = getPixel( inPixels, srcX, srcY+1, srcWidth, srcHeight );
					se = getPixel( inPixels, srcX+1, srcY+1, srcWidth, srcHeight );
				}
				outPixels[x] = ImageMath.bilinearInterpolate(xWeight, yWeight, nw, ne, sw, se);
			}
			if(y < height){
			    PixelUtils.setLineRGB(dst, y, width, outPixels);
			}
		}
		return dst;
	}

	final private int getPixel( int[] pixels, int x, int y, int width, int height ) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			switch (edgeAction) {
			case ZERO:
			default:
				return 0;
			case WRAP:
				return pixels[(ImageMath.mod(y, height) * width) + ImageMath.mod(x, width)];
			case CLAMP:
				return pixels[(ImageMath.clamp(y, 0, height-1) * width) + ImageMath.clamp(x, 0, width-1)];
			case RGB_CLAMP:
				return pixels[(ImageMath.clamp(y, 0, height-1) * width) + ImageMath.clamp(x, 0, width-1)] & 0x00ffffff;
			}
		}
		return pixels[ y*width+x ];
	}

}
