package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.DataSet;

/**
 * Modified for use with segments...
 * 
 * The <code>DataSetError</code> is a basic interface that specifies all methods
 * needed to read and modify data point error. This interface is kept most
 * general. However, derived classes may have dummy implementation for error
 * types that are not relevant. For plotting speed improvement this
 * simplification can/should be indicated via the
 *
 * @author rstein
 * @author Karl Duderstadt
 */
public interface DataSetWithSegments extends DataSet {

    public double[] getXSegmentValues();

    public double[] getYSegmentValues();
    
    public double getSegmentX(final int index);
    
    public double getSegmentY(final int index);
}
