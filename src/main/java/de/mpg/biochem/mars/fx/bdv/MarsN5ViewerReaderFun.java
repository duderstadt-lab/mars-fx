package de.mpg.biochem.mars.fx.bdv;

import ij.IJ;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;

import java.io.IOException;
import java.util.function.Function;

public class MarsN5ViewerReaderFun implements Function<String, N5Reader> {

    public String message;

    @Override
    public N5Reader apply(final String n5PathIn) {

        N5Reader n5;
        if (n5PathIn == null || n5PathIn.isEmpty())
            return null;

        final String rootPath;
        if (n5PathIn.contains(".h5") || n5PathIn.contains(".hdf5"))
            rootPath = N5Importer.h5DatasetPath(n5PathIn, true);
        else
            rootPath = n5PathIn;

        MarsN5Factory factory = new MarsN5Factory();
        try {
            n5 = factory.openReader(rootPath);
        } catch (IOException e) {
            IJ.handleException(e);
            return null;
        }
        return n5;
    }
}