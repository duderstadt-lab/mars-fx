package de.mpg.biochem.mars.fx.bdv;

import bdv.util.AbstractSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileTypeMatcher;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.function.Supplier;

public class MarsN5Source< T extends NumericType< T > > extends AbstractSource< T >
{

	protected final RandomAccessibleInterval< T >[] images;

	protected final AffineTransform3D[] transforms;

	public MarsN5Source(
			final T type,
			final String name,
			final RandomAccessibleInterval< T >[] images,
			final AffineTransform3D[] transforms )
	{
		super( type, name );
		this.images = images;
		this.transforms = transforms;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		RandomAccessibleInterval< T > img = Views.hyperSlice(images[ level ], images[ level ].numDimensions() - 1, t);
		//For now we assume time is the last axis and reslice accordingly
		if (img.numDimensions() > 2)
			return img; 
		else
			return Views.addDimension( img , 0, 0);
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( transforms[ t ] );
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return null;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return images.length;
	}

	public < V extends Volatile< T > & NumericType< V > > MarsN5VolatileSource< T, V > asVolatile( final V vType, final SharedQueue queue )
	{
		return new MarsN5VolatileSource<>( this, vType, queue );
	}

	public < V extends Volatile< T > & NumericType< V > > MarsN5VolatileSource< T, V > asVolatile(final Supplier< V > vTypeSupplier, final SharedQueue queue )
	{
		return new MarsN5VolatileSource<>( this, vTypeSupplier, queue );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public < V extends Volatile< T > & NumericType< V > > MarsN5VolatileSource< T, V > asVolatile( final SharedQueue queue )
	{
		final T t = getType();
		if ( t instanceof NativeType )
			return new MarsN5VolatileSource<>( this, ( V ) VolatileTypeMatcher.getVolatileTypeForType( ( NativeType )getType() ), queue );
		else
			throw new UnsupportedOperationException( "This method only works for sources of NativeType." );
	}
}
