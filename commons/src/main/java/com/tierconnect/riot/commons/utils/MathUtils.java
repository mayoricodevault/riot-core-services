package com.tierconnect.riot.commons.utils;

import java.util.List;

public class MathUtils
{
	public static boolean isPointInsidePolygon( List<double[]> points, double x, double y )
	{
		boolean inside = false;

		int j = points.size() - 1;

		for( int i = 0; i < points.size(); j = i, i++ )
		{
			double[] u0 = points.get( i );
			double[] u1 = points.get( j );

			if( y < u1[1] )
			{
				if( u0[1] <= y )
				{
					if( (y - u0[1]) * (u1[0] - u0[0]) > (x - u0[0]) * (u1[1] - u0[1]) )
					{
						inside = !inside;
					}
				}
			}
			else if( y < u0[1] )
			{
				if( (y - u0[1]) * (u1[0] - u0[0]) < (x - u0[0]) * (u1[1] - u0[1]) )
				{
					inside = !inside;
				}
			}
		}

		return inside;
	}

	static public double[] calculateCentroid( List<double[]> points )
	{
		double xsum = 0;
		double ysum = 0;
		for( int i = 0; i < points.size(); i++ )
		{
			xsum += points.get( i )[0];
			ysum += points.get( i )[1];
		}
		return new double[] { xsum / points.size(), ysum / points.size() };
	}

}
