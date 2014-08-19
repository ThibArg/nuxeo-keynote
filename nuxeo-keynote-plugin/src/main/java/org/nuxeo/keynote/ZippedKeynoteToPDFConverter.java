/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.keynote;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.runtime.api.Framework;




import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/*  This class is called by the converter, as declared in the
 *  zippedKeynote2PDF-contrib.xml contribution. The command line
 *  is (WARNING: Quite sure that if we change the definition in the
 *  XML, we will forget to change it here...):
 *  curl --upload-file #{sourceFilePath} #{nodeServerUrl} -o #{targetFilePath}
 *
 *  Actually, to convert, the url is server:port/convert. We add this "convert"
 *  in the call
 *
 *  The only parameter the caller must provide is a targetFileName (that will
 *  be used to build targetFilePath). But targetFileName is optional (we
 *  provide a random name if needed)
 */
public class ZippedKeynoteToPDFConverter extends CommandLineBasedConverter {

    public static final Log log = LogFactory.getLog(ZippedKeynoteToPDFConverter.class);

    //static boolean gNodeServerLooksAvailable = false;

    public static final String kKEYNOTE2PDF_NODEJS_SERVER_URL = Framework.getProperty(ZippedKeynoteToPDFConstants.KEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME);

    protected static boolean configParamsAreOk = false;

    /*  Check environment
     *  We wanted to check the availability once for all.
     *  But after all, it's ok for the distant Mac/nodejs server to be started later,
     *  so we comment this part.
     */
    public ZippedKeynoteToPDFConverter() throws Exception {
        super();

        if(kKEYNOTE2PDF_NODEJS_SERVER_URL == null || kKEYNOTE2PDF_NODEJS_SERVER_URL.isEmpty()) {
            log.error("Keynote to PDF conversions will fail: nodejs server url not defined. Check nuxeo.conf and the " + ZippedKeynoteToPDFConstants.KEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME + " key.");
        } else {
            configParamsAreOk = true;
        }

        /*
        *  We wanted to check the availability of the server once for all.
        *  But after all, it's ok for the distant Mac/nodejs server to be started later,
        *  after this class is initialized. so we comment this part.
        */
        /*
        if(configParamsAreOk) {
            URL url = new URL( kKEYNOTE2PDF_NODEJS_SERVER_URL + "/just_checking" );
            HttpURLConnection httpConn =  (HttpURLConnection)url.openConnection();
            httpConn.setInstanceFollowRedirects( false );
            httpConn.setRequestMethod( "HEAD" );
            try{
                httpConn.connect();
                gNodeServerLooksAvailable = httpConn.getResponseCode() == 200;
            }catch(java.net.ConnectException e){
                gNodeServerLooksAvailable = false;
            }
        }
        */
    }

    /**
     * Just checking if the file starts with %PDF.
     * If it is the case, it does not make sure the pdf is
     * well formated, complete, etc. etc.
     *
     * @since 5.9.5
     * @param inFile
     * @return boolean
     */
    protected boolean looksLikePdf(File inFile) {
        boolean startsWithCorrectTag = false;
        try {
            byte[] buffer = new byte[4];
            InputStream is = new FileInputStream(inFile);
            if (is.read(buffer) != buffer.length) {
                startsWithCorrectTag = false;
            } else if(   buffer[0] != 0x25 // %
                      || buffer[1] != 0x50 // P
                      || buffer[2] != 0x44 // D
                      || buffer[3] != 0x46)// F
            {
                startsWithCorrectTag = false;
                // So we have an error => let's log this error
                java.nio.file.Path path = Paths.get(inFile.getPath());
                log.error("nodejs server error:\n" + Files.readAllLines(path, StandardCharsets.UTF_8));

            } else {
                startsWithCorrectTag = true;
            }
            is.close();

        } catch (Exception e) {
            //FileNotFoundException, IOException
            log.error("Cannot read received pdf", e);
            startsWithCorrectTag = false;
        }

        return startsWithCorrectTag;
    }

    /**
     * In our context:
     *  - We have only one file
     *  - The distant nodejs server can fail converting the presentation.
     *    In that case, it returns a text error, and curl (on our side)
     *    just output it in the file, which is supposed to be a pdf. So,
     *    we now have a file, identified as pdf but is not a pdf, so it
     *    will lead to errors later (fulltext extraction, preview, ...)
     *
     *    So.
     *
     *    We should change the whole converter, and use something else than
     *    a commandline convertor. We should handle the connection, send
     *    request, get back the pdf ourselves, so we can handle errors,
     *    headers, etc.
     *
     *    Waiting for that (this is a joke, right?), lets just check the
     *    file and return null it it's not a pdf. This may break something
     *    in the call chain. In our plug-in, no error will be thrown and
     *    the document will just not have the "ZippedKeynote" facet and no
     *    pdf in the kn2pdf schema. So we can say that if the nodejs server
     *    returns an error, our plugin wil fail silently.
     *    Which may be not good, but it's better than having a file handled
     *    as a pdf whihc is not a pdf.
     */
    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) {

        String outputPath = cmdParams.getParameters().get("outDirPath");
        File outputDir = new File(outputPath);
        File[] files = outputDir.listFiles();
        List<Blob> blobs = new ArrayList<Blob>();

        boolean allGood = true;
        for (File file : files) {
            Blob blob = null;
            if(looksLikePdf(file)) {
                blob = new FileBlob(file);
                blob.setFilename(file.getName());
                blobs.add(blob);
            } else {
                allGood = false;
                break;
            }
        }
        if(allGood) {
            return new SimpleCachableBlobHolder(blobs);
        } else {
            return null;
        }
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        try {
            cmdBlobParams.put("sourceFilePath", blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        // Don't move if there is no configuration
        if(!configParamsAreOk) {
            throw new ConversionException("nodejs server (used for conversion) url is not defined. Check nuxeo.conf and the " + ZippedKeynoteToPDFConstants.KEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME + " key.");
        }

        Map<String, String> cmdStringParams = new HashMap<String, String>();

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append("kn2pdf_" + System.currentTimeMillis());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException("Unable to create tmp dir for transformer output");
        }

        cmdStringParams.put("outDirPath", outDir.getAbsolutePath());

        String targetFileName = null;
        if(parameters.containsKey("targetFileName")) {
            targetFileName = parameters.get("targetFileName").toString();
        }
        if(targetFileName == null || targetFileName.isEmpty()) {
            targetFileName = java.util.UUID.randomUUID().toString() + ".pdf";
        }
        cmdStringParams.put("targetFilePath",
                outDir.getAbsolutePath() + System.getProperty("file.separator") + targetFileName);

        cmdStringParams.put("nodeServerUrl", kKEYNOTE2PDF_NODEJS_SERVER_URL + "/convert/");

        return cmdStringParams;
    }

}
