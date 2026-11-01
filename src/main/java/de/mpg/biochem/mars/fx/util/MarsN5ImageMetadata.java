/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2026 Karl Duderstadt
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
package de.mpg.biochem.mars.fx.util;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import de.mpg.biochem.mars.metadata.MarsBdvSource;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.metadata.MarsOMEImage;
import de.mpg.biochem.mars.metadata.MarsOMEPlane;
import de.mpg.biochem.mars.n5.MarsN5ViewerReaderFun;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.PositiveInteger;

/**
 * Builds the OME image entry of a metadata record from the dimensions of an
 * N5/Zarr dataset, for records created from N5 data rather than from an
 * opened image. Mars writes datasets as XY[Z]CT, so the sizes are read from
 * that order and one plane is made per Z, C and T. Planes carry no timing
 * (deltaT stays unset), so tracked tables get a T column but no Time_(s)
 * column until timing is added to the record.
 *
 * <p>Peak and object tracking need this entry: they read the image's sizes
 * and look up every plane's time.
 */
public final class MarsN5ImageMetadata {

	private MarsN5ImageMetadata() {}

	/**
	 * Sizes {X, Y, Z, C, T} for dataset dimensions in XY[Z]CT order: XY,
	 * XYT, XYCT or XYZCT.
	 */
	public static int[] sizesXYZCT(long[] dims) {
		int[] s = { 1, 1, 1, 1, 1 };
		if (dims.length > 0) s[0] = (int) dims[0];
		if (dims.length > 1) s[1] = (int) dims[1];
		if (dims.length == 3) s[4] = (int) dims[2];
		else if (dims.length == 4) {
			s[3] = (int) dims[2];
			s[4] = (int) dims[3];
		}
		else if (dims.length >= 5) {
			s[2] = (int) dims[2];
			s[3] = (int) dims[3];
			s[4] = (int) dims[4];
		}
		return s;
	}

	/** An image (index 0) with one plane per Z, C and T and no timing. */
	public static MarsOMEImage createImage(long[] dims, String name) {
		int[] s = sizesXYZCT(dims);
		MarsOMEImage image = new MarsOMEImage();
		image.setImageID(0);
		image.setID("Image:0");
		if (name != null) image.setName(name);
		image.setDimensionOrder(DimensionOrder.XYZCT);
		image.setSizeX(new PositiveInteger(Math.max(1, s[0])));
		image.setSizeY(new PositiveInteger(Math.max(1, s[1])));
		image.setSizeZ(new PositiveInteger(Math.max(1, s[2])));
		image.setSizeC(new PositiveInteger(Math.max(1, s[3])));
		image.setSizeT(new PositiveInteger(Math.max(1, s[4])));
		for (int z = 0; z < s[2]; z++)
			for (int c = 0; c < s[3]; c++)
				for (int t = 0; t < s[4]; t++) {
					int planeIndex = (int) image.getPlaneIndex(z, c, t);
					image.setPlane(new MarsOMEPlane(image, 0, planeIndex,
						new NonNegativeInteger(z), new NonNegativeInteger(c),
						new NonNegativeInteger(t)), z, c, t);
				}
		return image;
	}

	/**
	 * Give a record with no image one built from these dimensions.
	 *
	 * @return true if an image was added
	 */
	public static boolean ensureImage(MarsMetadata metadata, long[] dims,
		String name)
	{
		if (metadata.getImageCount() > 0 || dims == null) return false;
		metadata.setImage(createImage(dims, name), 0);
		return true;
	}

	/**
	 * Give a record with no image one built from the dimensions of the
	 * N5/Zarr dataset its first N5 BDV source points at.
	 *
	 * @return true if an image was added
	 */
	public static boolean ensureImage(MarsMetadata metadata) {
		if (metadata.getImageCount() > 0) return false;
		for (MarsBdvSource source : metadata.getBdvSources()) {
			if (!source.isN5()) continue;
			try {
				N5Reader reader = new MarsN5ViewerReaderFun().apply(source.getPath());
				DatasetAttributes attributes = reader.getDatasetAttributes(source
					.getN5Dataset());
				reader.close();
				if (attributes == null) continue;
				return ensureImage(metadata, attributes.getDimensions(), source
					.getN5Dataset());
			}
			catch (Exception e) {
				// try the next source
			}
		}
		return false;
	}
}
