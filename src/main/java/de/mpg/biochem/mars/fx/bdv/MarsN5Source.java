/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2021 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
