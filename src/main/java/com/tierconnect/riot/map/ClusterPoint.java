package com.tierconnect.riot.map;

public class ClusterPoint{
	private final int SEMI_BIG_PRIME = 15487469;
	
	public ClusterPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public ClusterPoint(int x, int y,float realX,float realY) {
		this.x = x;
		this.y = y;
		this.realX = realX;
		this.realY = realY;
	}
	private int x,y;
	private float realX,realY;
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public float getRealX() {
		return realX;
	}
	public float getRealY() {
		return realY;
	}

	@Override
	public int hashCode(){
		return x * SEMI_BIG_PRIME + y;
	}

	@Override
	public boolean equals(Object o){
		ClusterPoint b = (ClusterPoint) o;
		return this.x == b.getX() && this.y == b.getY();
	}
}