package de.mpg.biochem.mars.fx.bdv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import com.amazonaws.client.builder.AwsClientBuilder;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageReader;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageWriter;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;

/**
 * Copy of N5Factory from Saalfeld lab, HHMI Janelia. BSD-2.
 *
 * Factory for various N5 readers and writers.  Implementation specific
 * parameters can be provided to the factory instance and will be used when
 * such implementations are generated and ignored otherwise. Reasonable
 * defaults are provided.
 *
 * This copy allows for a custom s3 endpoint to be added for AWS paths.
 * @author Karl Duderstadt
 *
 * @author Stephan Saalfeld
 * @author John Bogovic
 * @author Igor Pisarev
 */
public class MarsN5Factory implements Serializable {

    private static final long serialVersionUID = -6823715427289454617L;

    private static byte[] HDF5_SIG = {(byte)137, 72, 68, 70, 13, 10, 26, 10};
    private int[] hdf5DefaultBlockSize = {64, 64, 64, 1, 1};
    private boolean hdf5OverrideBlockSize = false;
    private GsonBuilder gsonBuilder = new GsonBuilder();
    private String zarrDimensionSeparator = ".";
    private boolean zarrMapN5DatasetAttributes = true;
    private String googleCloudProjectId = null;

    public MarsN5Factory hdf5DefaultBlockSize(final int... blockSize) {

        hdf5DefaultBlockSize = blockSize;
        return this;
    }

    public MarsN5Factory hdf5OverrideBlockSize(final boolean override) {

        hdf5OverrideBlockSize = override;
        return this;
    }

    public MarsN5Factory gsonBuilder(final GsonBuilder gsonBuilder) {

        this.gsonBuilder = gsonBuilder;
        return this;
    }

    public MarsN5Factory zarrDimensionSeparator(final String separator) {

        zarrDimensionSeparator = separator;
        return this;
    }

    public MarsN5Factory zarrMapN5Attributes(final boolean mapAttributes) {

        zarrMapN5DatasetAttributes = mapAttributes;
        return this;
    }

    public MarsN5Factory googleCloudProjectId(final String projectId) {

        googleCloudProjectId = projectId;
        return this;
    }

    public static boolean isHDF5Writer(final String path) {

        if (path.contains(".h5") || path.contains(".hdf5"))
            return true;
        else
            return false;
    }

    public static boolean isHDF5Reader(final String path) throws FileNotFoundException, IOException {

        if (Files.isRegularFile(Paths.get(path))) {
            /* optimistic */
            if (path.matches("(?i).*\\.(h5|hdf5)"))
                return true;
            else {
                try (final FileInputStream in = new FileInputStream(new File(path))) {
                    final byte[] sig = new byte[8];
                    in.read(sig);
                    return Arrays.equals(sig, HDF5_SIG);
                }
            }
        }
        return false;
    }

    /**
     * Helper method.
     *
     * @param url
     * @return
     */
    private static AmazonS3 createS3(final String url) {

        AmazonS3 s3;
        AWSCredentials credentials = null;
        try {
            credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
        } catch(final Exception e) {
            System.out.println( "Could not load AWS credentials, falling back to anonymous." );
        }
        final AWSStaticCredentialsProvider credentialsProvider =
                new AWSStaticCredentialsProvider(credentials == null ? new AnonymousAWSCredentials() : credentials);

        final AmazonS3URI uri = new AmazonS3URI(url);
        final Optional<String> region = Optional.ofNullable(uri.getRegion());

        if(region.isPresent()) {
            s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(credentialsProvider)
                    .withRegion(region.map(Regions::fromName).orElse(Regions.US_EAST_1))
                    .build();
        } else {
            s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(credentialsProvider)
                    .build();
        }

        return s3;
    }

    /**
     * Helper method.
     *
     * @param endpoint
     * @return
     */
    private static AmazonS3 createS3WithEndpoint(final String endpoint) {
        AmazonS3 s3;
        AWSCredentials credentials = null;
        try {
            credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
        } catch(final Exception e) {
            System.out.println( "Could not load AWS credentials, falling back to anonymous." );
        }
        final AWSStaticCredentialsProvider credentialsProvider =
                new AWSStaticCredentialsProvider(credentials == null ? new AnonymousAWSCredentials() : credentials);

        //US_EAST_2 is used as a dummy region.
        s3 = AmazonS3ClientBuilder.standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, Regions.US_EAST_2.getName()))
                .withCredentials(credentialsProvider)
                .build();

        return s3;
    }

    /**
     * Open an {@link N5Reader} for N5 filesystem.
     *
     * @param path path to the n5 root folder
     * @return the N5FsReader
     * @throws IOException the io exception
     */
    public N5FSReader openFSReader(final String path) throws IOException {

        return new N5FSReader(path, gsonBuilder);
    }

    /**
     * Open an {@link N5Reader} for Zarr.
     *
     * For more options of the Zarr backend study the {@link N5ZarrReader}
     * constructors.
     *
     * @param path path to the zarr directory
     * @return the N5ZarrReader
     * @throws IOException the io exception
     */
    public N5ZarrReader openZarrReader(final String path) throws IOException {

        return new N5ZarrReader(path, gsonBuilder, zarrDimensionSeparator, zarrMapN5DatasetAttributes);
    }

    /**
     * Open an {@link N5Reader} for HDF5. Close the reader when you do not need
     * it any more.
     *
     * For more options of the HDF5 backend study the {@link N5HDF5Reader}
     * constructors.
     *
     * @param path path to the hdf5 file
     * @return the N5HDF5Reader
     * @throws IOException the io exception
     */
    public N5HDF5Reader openHDF5Reader(final String path) throws IOException {

        return new N5HDF5Reader(path, hdf5OverrideBlockSize, gsonBuilder, hdf5DefaultBlockSize);
    }

    /**
     * Open an {@link N5Reader} for Google Cloud.
     *
     * @param url url to the google cloud object
     * @return the N5GoogleCloudStorageReader
     * @throws IOException the io exception
     */
    public N5GoogleCloudStorageReader openGoogleCloudReader(final String url) throws IOException {

        final GoogleCloudStorageClient storageClient = new GoogleCloudStorageClient();
        final Storage storage = storageClient.create();
        final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(url);

        return new N5GoogleCloudStorageReader(
                storage,
                googleCloudUri.getBucket(),
                googleCloudUri.getKey(),
                gsonBuilder);
    }

    /**
     * Open an {@link N5Reader} for AWS S3.
     *
     * @param url url to the amazon s3 object
     * @return the N5AmazonS3Reader
     * @throws IOException the io exception
     */
    public N5AmazonS3Reader openAWSS3Reader(final String url) throws IOException {
        return new N5AmazonS3Reader(
                createS3(url),
                new AmazonS3URI(url),
                gsonBuilder);
    }

    /**
     * Open an {@link N5Reader} for AWS S3.
     *
     * @param s3Url url to the amazon s3 object
     * @param endpointUrl endpoint url for the server
     * @return the N5AmazonS3Reader
     * @throws IOException the io exception
     */
    public N5AmazonS3Reader openAWSS3ReaderWithEndpoint(final String s3Url, final String endpointUrl) throws IOException {
        return new N5AmazonS3Reader(
                createS3WithEndpoint(endpointUrl),
                new AmazonS3URI(s3Url),
                gsonBuilder);
    }

    /**
     * Open an {@link N5Writer} for N5 filesystem.
     *
     * @param path path to the n5 directory
     * @return the N5FSWriter
     * @throws IOException the io exception
     */
    public N5FSWriter openFSWriter(final String path) throws IOException {
        return new N5FSWriter(path, gsonBuilder);
    }

    /**
     * Open an {@link N5Writer} for Zarr.
     *
     * For more options of the Zarr backend study the {@link N5ZarrWriter}
     * constructors.
     *
     * @param path path to the zarr directory
     * @return the N5ZarrWriter
     * @throws IOException the io exception
     */
    public N5ZarrWriter openZarrWriter(final String path) throws IOException {
        return new N5ZarrWriter(path, gsonBuilder, zarrDimensionSeparator, zarrMapN5DatasetAttributes);
    }

    /**
     * Open an {@link N5Writer} for HDF5.  Don't forget to close the writer
     * after writing to close the file and make it available to other
     * processes.
     *
     * For more options of the HDF5 backend study the {@link N5HDF5Writer}
     * constructors.
     *
     * @param path path to the hdf5 file
     * @return the N5HDF5Writer
     * @throws IOException the io exception
     */
    public N5HDF5Writer openHDF5Writer(final String path) throws IOException {
        return new N5HDF5Writer(path, hdf5OverrideBlockSize, gsonBuilder, hdf5DefaultBlockSize);
    }

    /**
     * Open an {@link N5Writer} for Google Cloud.
     *
     * @param url url to the google cloud object
     * @return the N5GoogleCloudStorageWriter
     * @throws IOException the io exception
     */
    public N5GoogleCloudStorageWriter openGoogleCloudWriter(final String url) throws IOException {
        final GoogleCloudStorageClient storageClient;
        if (googleCloudProjectId == null) {
            final ResourceManager resourceManager = new GoogleCloudResourceManagerClient().create();
            final Iterator<Project> projectsIterator = resourceManager.list().iterateAll().iterator();
            if (!projectsIterator.hasNext())
                return null;
            storageClient = new GoogleCloudStorageClient(projectsIterator.next().getProjectId());
        } else
            storageClient = new GoogleCloudStorageClient(googleCloudProjectId);

        final Storage storage = storageClient.create();
        final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(url);
        return new N5GoogleCloudStorageWriter(
                storage,
                googleCloudUri.getBucket(),
                googleCloudUri.getKey(),
                gsonBuilder);
    }

    /**
     * Open an {@link N5Writer} for AWS S3.
     *
     * @param url url to the s3 object
     * @return the N5AmazonS3Writer
     * @throws IOException the io exception
     */
    public N5AmazonS3Writer openAWSS3Writer(final String url) throws IOException {

        return new N5AmazonS3Writer(
                createS3(url),
                new AmazonS3URI(url),
                gsonBuilder);
    }

    /**
     * Open an {@link N5Writer} for AWS S3.
     *
     * @param s3Url url to the s3 object
     * @param endpointUrl endpoint url to the server
     * @return the N5AmazonS3Writer
     * @throws IOException the io exception
     */
    public N5AmazonS3Writer openAWSS3WriterWithEndpoint(final String s3Url, final String endpointUrl) throws IOException {

        return new N5AmazonS3Writer(
                createS3WithEndpoint(endpointUrl),
                new AmazonS3URI(s3Url),
                gsonBuilder);
    }

    /**
     * Open an {@link N5Reader} based on some educated guessing from the url.
     *
     * @param url the location of the root location of the store
     * @return the N5Reader
     * @throws IOException the io exception
     */
    public N5Reader openReader(final String url) throws IOException {
        try {
            final URI uri = new URI(url);
            final String scheme = uri.getScheme();
            if (scheme == null);
            else if (scheme.equals("s3")) {
                return openAWSS3Reader(url);
            } else if (scheme.equals("gs"))
                return openGoogleCloudReader(url);
            else if (uri.getHost()!= null && scheme.equals("https") || scheme.equals("http")) {
                if (uri.getHost().matches(".*s3\\.amazonaws\\.com"))
                    return openAWSS3Reader(url);
                else if (uri.getHost().matches(".*cloud\\.google\\.com") || uri.getHost().matches(".*storage\\.googleapis\\.com"))
                    return openGoogleCloudReader(url);
                else if (uri.getHost().matches(".*s3\\..*")) {
                    String[] parts = uri.getHost().split("\\.",3);
                    String bucket = parts[0];
                    String s3Url = "s3://" + bucket + uri.getPath();
                    String endpointUrl = uri.getScheme() + "://" + parts[2] + ":" + uri.getPort();
                    return openAWSS3ReaderWithEndpoint(s3Url, endpointUrl);
                }
            }
        } catch (final URISyntaxException e) {}
        if (isHDF5Reader(url))
            return openHDF5Reader(url);
        else if (url.contains(".zarr"))
            return openZarrReader(url);
        else
            return openFSReader(url);
    }

    /**
     * Open an {@link N5Writer} based on some educated guessing from the url.
     *
     * @param url the location of the root location of the store
     * @return the N5Writer
     * @throws IOException the io exception
     */
    public N5Writer openWriter(final String url) throws IOException {

        try {
            final URI uri = new URI(url);
            final String scheme = uri.getScheme();
            if (scheme == null);
            else if (scheme.equals("s3"))
                return openAWSS3Writer(url);
            else if (scheme.equals("gs"))
                return openGoogleCloudWriter(url);
            else if (uri.getHost() != null && scheme.equals("https") || scheme.equals("http")) {
                if (uri.getHost().matches(".*s3\\.amazonaws\\.com"))
                    return openAWSS3Writer(url);
                else if (uri.getHost().matches(".*cloud\\.google\\.com") || uri.getHost().matches(".*storage\\.googleapis\\.com"))
                    return openGoogleCloudWriter(url);
                else if (uri.getHost().matches(".*s3\\..*")) {
                    String[] parts = uri.getHost().split("\\.",3);
                    String bucket = parts[0];
                    String s3Url = "s3://" + bucket + uri.getPath();
                    String endpointUrl = uri.getScheme() + "://" + parts[2] + ":" + uri.getPort();
                    return openAWSS3WriterWithEndpoint(s3Url, endpointUrl);
                }
            }
        } catch (final URISyntaxException e) {}
        if (isHDF5Writer(url))
            return openHDF5Writer(url);
        else if (url.matches("(?i).*\\.zarr"))
            return openZarrWriter(url);
        else
            return openFSWriter(url);
    }
}