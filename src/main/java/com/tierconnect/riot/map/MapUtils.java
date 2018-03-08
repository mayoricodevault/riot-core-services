package com.tierconnect.riot.map;

import java.awt.geom.Path2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.tierconnect.riot.sdk.dao.UserException;

public class MapUtils {
	/**
	 * Ignore things without position 
	 * 
	 */
//	public static Map<ClusterPoint, Collection<CassandraThing>> clusterize(Collection<CassandraThing> things,float squareSize){
//		Map<ClusterPoint,Collection<CassandraThing> > clusters = new HashMap<>();
//		for(CassandraThing thing : things){
//			if(thing.getX() == null || thing.getY() == null){
//				continue;
//			}			
//
//			int x = (int) (thing.getX() / squareSize);
//			int y = (int) (thing.getY() / squareSize);
//
//			Collection<CassandraThing> list = clusters.get(new ClusterPoint(x,y));			
//			if(list == null){
//				list = new LinkedList<>();
//				ClusterPoint clusterPoint = new ClusterPoint(x, y,thing.getX(),thing.getY());
//				clusters.put(clusterPoint, list);
//			}
//
//			list.add(thing);
//		}
//		
//		return clusters;
//	}
	
	public static boolean insidePolygon(Collection<Float> xs,Collection<Float> ys,float x,float y){
		if(xs.size() != ys.size()){
			throw new UserException("Different number of points");
		}
		if(xs.size() <= 3){
			throw new UserException("Insufficient number of points");
		}
		
		Path2D.Float path = new Path2D.Float();		
		Iterator<Float> itX = xs.iterator();
		Iterator<Float> itY = ys.iterator();
		path.moveTo(itX.next(),itY.next());
		
	    while(itX.hasNext()){
	    	path.lineTo(itX.next(),itY.next());	
	    }	    
	    path.closePath();
	    return path.contains(x, y);
	}	
}
