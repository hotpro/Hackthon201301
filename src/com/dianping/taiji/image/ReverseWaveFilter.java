/**
 * Project: imcaptcha
 * 
 * File Created at 2013-1-17
 * $Id$
 * 
 * Copyright 2010 dianping.com.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.taiji.image;

import com.jabistudio.androidjhlabs.filter.TransformFilter;



/**
 * TODO Comment of WaveFilter
 * 
 * @author yihua.huang
 * 
 */
public class ReverseWaveFilter extends TransformFilter {

	private float factor = 50;

	private float radius = (float) (6000f / (Math.PI * 2));

	/**
	 * 
	 */
	public ReverseWaveFilter() {
		super();
	}

	/**
	 * @param factor
	 * @param radius
	 */
	public ReverseWaveFilter(float factor, float radius) {
		super();
		this.factor = factor;
		this.radius = radius;
	}

	@Override
	protected void transformInverse(int x, int y, float[] out) {
		out[0] = (float) (x + factor * Math.sin(y / radius));
		out[1] = y;
	}


}
