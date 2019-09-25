package de.mpg.biochem.mars.fx.plot.tools;

import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.spi.AbstractDataSet;

public abstract class AbstractSegmentDataSet<D extends AbstractSegmentDataSet<D>> extends AbstractDataSet<D> implements DataSetWithSegments {
		
		/**
		* Creates a new instance of <code>AbstractDataSet</code>.
		*
		* @param name
		*            of the DataSet
		* @throws IllegalArgumentException
		*             if <code>name</code> is <code>null</code>
		*/
		protected AbstractSegmentDataSet(final String name) {
			super(name);
		}
		
		@Override
		@SuppressWarnings("unchecked")
			protected D getThis() {
			return (D) this;
		}
		
		@Override
		public D lock() {
			lock.lock();
			return getThis();
		}
		
		@Override
		public D unlock() {
			lock.unlock();
			return getThis();
		}
		
		@Override
		public D fireInvalidated(final UpdateEvent event) {
			super.fireInvalidated(event);
			return getThis();
		}
		
}