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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

public class ZippedKeynoteToPDFUtils {

    public static final Log log = LogFactory.getLog(ZippedKeynoteToPDFUtils.class);

    public enum ZippedKeynoteStatus {
        UNKNOWN,
        IS_KEYNOTE,
        IS_NOT_KEYNOTE
    }

    Blob blob = null;
    ZippedKeynoteStatus status = ZippedKeynoteStatus.UNKNOWN;

    public ZippedKeynoteToPDFUtils(Blob inBlob) {
        blob = inBlob;
    }

    public boolean isZippedKeynote() throws Exception {

        if(status != ZippedKeynoteStatus.UNKNOWN) {
            return status == ZippedKeynoteStatus.IS_KEYNOTE;
        }

        status = ZippedKeynoteStatus.IS_NOT_KEYNOTE;
        if( blob != null
         && blob.getMimeType().equalsIgnoreCase("application/zip")) {
            ZipInputStream zis = new ZipInputStream(blob.getStream());
            ZipEntry zentry = null;
            String name;

            // We are reading from a stream, not a file, so we can't make sure
            // it reads the "first" file, and can't rely on the fact that the
            // "zip contains a Keynote file if one of the 2 first elements
            // is a .key file (second can be a __MACOS folder).
            // The workaround i to find an item which:
            //      -> Is a directory
            //      -> Ends with .key/
            //      -> And contains one signle / (just in case the Keynote format
            //         contain sub-.key files. Never seen that, but who knows)
            do {
                zentry = zis.getNextEntry();
                if(zentry != null) {
                    name = zentry.getName();
                    if(zentry.isDirectory()
                         && name.endsWith(".key/")
                         && name.indexOf('/') == (name.length() - 1)) {
                        status = ZippedKeynoteStatus.IS_KEYNOTE;
                    }
                    zis.closeEntry();
                }
            } while(zentry != null && status == ZippedKeynoteStatus.IS_NOT_KEYNOTE);
            zis.close();
        }

        return status == ZippedKeynoteStatus.IS_KEYNOTE;
    }

    /*
     */
    public Blob convert() throws Exception {
        Blob resultPDF = null;

        if(isZippedKeynote()) {
            ConversionService conversionService = Framework
                    .getService(ConversionService.class);

            BlobHolder source = new SimpleBlobHolder(blob);

            Map<String, Serializable> parameters = new HashMap<String, Serializable>();
            String targetFileName = blob.getFilename() + ".pdf";
            parameters.put("targetFileName", targetFileName);
            try {
                BlobHolder result = conversionService.convert("zippedKeynoteToPDF",
                        source, parameters);
                if(result != null && result.getBlob() != null) {
                    resultPDF = result.getBlob();
                    resultPDF.setFilename(targetFileName);
                    resultPDF.setMimeType("application/pdf");
                }

            } catch(Exception e) {
                log.error("Conversion error", e);
            }
        } else {
            // Should return a generic PDF stating "not a keynote presentation"?
            // Or call the generic "anytopdf" converter?
            // Or just leave the resultPDF null, so the caller handles it?
        }

        return resultPDF;
    }
}
