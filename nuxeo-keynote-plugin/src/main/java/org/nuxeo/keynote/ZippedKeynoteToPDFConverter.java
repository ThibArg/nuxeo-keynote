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

/*  This class is called by the converter, as declared in the
 *  zippedKeynote2PDF-contrib.xml contribution. The command line
 *  is (WARNING: Quite sure that if we change the definition in the
 *  XML, we will forget to change it here...):
 *  curl --upload-file #{sourceFilePath} #{nodeServerUrl} -o #{targetFilePath}
 *
 *  The only parameter the caller must provide is a targetFileName (that will
 *  be used to build targetFilePath). But targetFileName is optionnal (we
 *  provide a random name if needed)
 */
public class ZippedKeynoteToPDFConverter extends CommandLineBasedConverter {

    public static final Log log = LogFactory.getLog(ZippedKeynoteToPDFConverter.class);

    //static boolean gNodeServerLooksAvailable = false;

    public static final String kKEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME = "keynote2pdf.nodejs.server.urlAndPort";
    public static final String kKEYNOTE2PDF_NODEJS_SERVER_URL = Framework.getProperty(kKEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME);
    public static final String KEYNOTE2PDF_NODEJS_SERVER_TOKEN_NAME= "keynote2pdf.nodejs.server.token";
    public static final String KEYNOTE2PDF_NODEJS_SERVER_TOKEN = Framework.getProperty(KEYNOTE2PDF_NODEJS_SERVER_TOKEN_NAME);

    /*  We wanted to check the avilability once for all.
     *  But after all, it's ok for the distant Mac/nodejs server to be started later,
     *  so we comment this part.
     */
    /*
    public ZippedKeynoteToPDFConverter() throws Exception {
        super();

        if(kKEYNOTE2PDF_NODEJS_SERVER_URL == null || kKEYNOTE2PDF_NODEJS_SERVER_URL.isEmpty()) {
            gNodeServerLooksAvailable = false;
            log.error("Keynote to PDF conversions will fail: nodejs server url not defined. Check nuxeo.conf and the " + kKEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME + " key.");
        } else {
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
    }
    */

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) {

        String outputPath = cmdParams.getParameters().get("outDirPath");
        File outputDir = new File(outputPath);
        File[] files = outputDir.listFiles();
        List<Blob> blobs = new ArrayList<Blob>();

        for (File file : files) {
            Blob blob = new FileBlob(file);
            blob.setFilename(file.getName());
            blobs.add(blob);
        }
        return new SimpleCachableBlobHolder(blobs);
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
        if(kKEYNOTE2PDF_NODEJS_SERVER_URL == null || kKEYNOTE2PDF_NODEJS_SERVER_URL.isEmpty()) {
            //log.error("nodejs server url not defined. Check nuxeo.conf and the " + kNKEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME + " key.");
            throw new ConversionException("Conversion will fail: nodejs server url is not defined. Check nuxeo.conf and the " + kKEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME + " key.");
        }
        if(KEYNOTE2PDF_NODEJS_SERVER_TOKEN == null || KEYNOTE2PDF_NODEJS_SERVER_TOKEN.isEmpty()) {
            //log.error("nodejs server url not defined. Check nuxeo.conf and the " + KEYNOTE2PDF_NODEJS_SERVER_TOKEN_NAME + " key.");
            throw new ConversionException("Conversion will fail: nodejs server token is not defined. Check nuxeo.conf and the " + KEYNOTE2PDF_NODEJS_SERVER_TOKEN_NAME + " key.");
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

        cmdStringParams.put("nodeServerUrl", kKEYNOTE2PDF_NODEJS_SERVER_URL);
        cmdStringParams.put("headerToken", KEYNOTE2PDF_NODEJS_SERVER_TOKEN);

        return cmdStringParams;
    }

}
