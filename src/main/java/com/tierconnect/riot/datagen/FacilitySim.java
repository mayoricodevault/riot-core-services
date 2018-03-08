package com.tierconnect.riot.datagen;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;

public class FacilitySim
{
	Random r = new Random();

	long epoch;

	ThingTypeField number;
	ThingTypeField color;
	ThingTypeField mood;
	ThingTypeField udf4;
	ThingTypeField udf5;

	ThingType ttPallet;
	ThingType ttCarton;
	ThingType ttBox;
	ThingType ttAsset;
	ThingType ttTag;

	StackOfBoxes stackOfBoxes;
	Conveyor conveyor;
	ThingLoader cartonLoader;
	ThingLoader palletLoader;
	TruckLoader truckLoader;

	Set<Thing> changedThings;

	public FacilitySim()
	{
		changedThings = new LinkedHashSet<Thing>();
	}

	// class Order
	// {
	// int numShoes;
	// int numPants;
	// int numJackets;
	// }

	class StackOfBoxes
	{
		int min = 3;
		int max = 10;
		LinkedList<Thing> list;

		public StackOfBoxes( long simtime )
		{
			list = new LinkedList<Thing>();
			replenish( simtime );
		}

		public Thing getNext( long simtime )
		{
			Thing box = list.pop();
			box.setUdfValue( simtime, "state", "unfolded" );
			// box.setUdfValue( simtime, "zone", "stack" );
			changedThings.add( box );

			if( list.size() <= min )
			{
				replenish( simtime );
			}

			return box;
		}

		private void replenish( long simtime )
		{
			while( list.size() <= max )
			{
				Thing box = new Thing( simtime, ttBox, 0, null );
				box.setUdfValue( simtime, "state", "folded" );
				box.setUdfValue( simtime, "zone", "stack" );
				list.add( box );
				changedThings.add( box );
			}
		}
	}

	class Conveyor
	{
		int max = 10;

		LinkedList<Thing> list;

		public Conveyor()
		{
			list = new LinkedList<Thing>();
		}

		public void advanceLine( long simtime )
		{
			list.push( null );
		}

		public void addBox( long simtime, Thing box )
		{
			list.set( 0, box );
			box.setUdfValue( simtime, "zone", "conveyor" );
			changedThings.add( box );
		}

		public void fillBoxes( long simtime )
		{
			for( int i = 0; i < list.size(); i++ )
			{
				Thing box = list.get( i );
				int c;
				switch( i )
				{

				// add a random number of shoes
				case 1:
					c = r.nextInt( 3 );
					c = 3;
					for( int n = 0; n < c; n++ )
					{
						Thing asset = new Thing( simtime, ttAsset, 0, box );
						asset.setUdfValue( simtime, "type", "shoe" );
						asset.setUdfValue( simtime, "color", "red" );
						changedThings.add( asset );
					}
					break;

				// add a random number of pants
				case 2:
					c = r.nextInt( 3 );
					c = 3;
					for( int n = 0; n < c; n++ )
					{
						Thing asset = new Thing( simtime, ttAsset, 0, box );
						asset.setUdfValue( simtime, "type", "pants" );
						asset.setUdfValue( simtime, "color", "green" );
						changedThings.add( asset );
					}
					break;

				// add a random number of jackets
				case 3:
					c = r.nextInt( 3 );
					c = 3;
					for( int n = 0; n < c; n++ )
					{
						Thing asset = new Thing( simtime, ttAsset, 0, box );
						asset.setUdfValue( simtime, "type", "jacket" );
						asset.setUdfValue( simtime, "color", "blue" );
						changedThings.add( asset );
					}
					break;

				default:
					break;
				}
			}
		}

		public Thing getBox( long simtime )
		{
			if( list.size() >= max )
			{
				Thing box = list.removeLast();
				box.setUdfValue( simtime, "state", "closed" );
				changedThings.add( box );
				return box;
			}
			else
			{
				return null;
			}
		}
	}

	// loads smaller things into larger things.
	// e.g., boxes into cartons, cartons on to pallets.
	class ThingLoader
	{
		int max;
		ThingType parentThingType;
		Thing parent;
		int count;

		public ThingLoader( ThingType parentThingType )
		{
			this.parentThingType = parentThingType;
			max = 4;
			count = 0;
		}

		public void addChild( long simtime, Thing child )
		{
			if( parent == null )
			{
				parent = new Thing( simtime, parentThingType, 0, null );
			}
			count++;
			child.parent = parent;
		}

		// returns the parent thing when it is full
		public Thing getParent( long simtime )
		{
			if( count > max )
			{
				changedThings.add( parent );
				Thing retval = parent;
				parent = null;
				count = 0;
				return retval;
			}
			return null;
		}
	}

	class TruckLoader
	{
		public TruckLoader()
		{

		}

		public void shipPallet( long simtime, Thing pallet )
		{

		}
	}

	public Set<Thing> init( long simtime )
	{
		changedThings.clear();
		
		ThingTypeField state = Utils.getThingTypeField( "state" );
		ThingTypeField zone = Utils.getThingTypeField( "zone" );
		ThingTypeField type = Utils.getThingTypeField( "type" );
		ThingTypeField number = Utils.getThingTypeField( "number" );
		ThingTypeField color = Utils.getThingTypeField( "color" );
		ThingTypeField udf4 = Utils.getThingTypeField( "udf4" );
		ThingTypeField udf5 = Utils.getThingTypeField( "udf5" );

		ttPallet = Utils.getThingType( "Pallet", new ThingTypeField[] { state, color, zone, udf4, udf5 } );
		ttCarton = Utils.getThingType( "Carton", new ThingTypeField[] { state, color, zone, udf4, udf5 } );
		ttBox = Utils.getThingType( "Box", new ThingTypeField[] { state, color, zone, udf4, udf5 } );
		ttAsset = Utils.getThingType( "Asset", new ThingTypeField[] { type, color, zone } );
		ttTag = Utils.getThingType( "Tag", new ThingTypeField[] { number, color, zone, udf4, udf5 } );

		stackOfBoxes = new StackOfBoxes( simtime );
		conveyor = new Conveyor();
		cartonLoader = new ThingLoader( ttCarton );
		palletLoader = new ThingLoader( ttPallet );
		truckLoader = new TruckLoader();
		
		return changedThings;
	}

	public Set<Thing> step( long simtime )
	{
		changedThings.clear();

		// unfold box
		Thing newbox1 = stackOfBoxes.getNext( simtime );

		conveyor.advanceLine( simtime );
		
		// put the box on the conveyor belt
		conveyor.addBox( simtime, newbox1 );

		// advance and fill boxes on conveyor
		conveyor.fillBoxes( simtime );

		// remove box from conveyor and give to carton loader
		Thing newbox2 = conveyor.getBox( simtime );
		if( newbox2 != null )
		{
			newbox2.setUdfValue( simtime, "zone", "CartonLoader" );
			cartonLoader.addChild( simtime, newbox2 );
		}

		// when carton is full place on pallet
		Thing newcarton = cartonLoader.getParent( simtime );
		if( newcarton != null )
		{
			newcarton.setUdfValue( simtime, "zone", "PalletLoader" );
			palletLoader.addChild( simtime, newcarton );
		}

		// when pallet is full place on truck
		Thing pallet = palletLoader.getParent( simtime );
		if( pallet != null )
		{
			pallet.setUdfValue( simtime, "zone", "TruckLoader" );
			truckLoader.shipPallet( simtime, pallet );
		}

		return changedThings;
	}
}
