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

/**
 * A utility, to be reused elsewhere in the code.
 * <p>
 * Basically, contains a utility (isZippedKeynote)which checks if a blob contains
 * a zip file which itself contains a Keynote presentation, and another (convert)
 * which, well, converts this blo and returns a pdf
 *      -
 * @author Thibaud Arguillere
 *
 * @since 5.9.5
 */
public class ZippedKeynoteToPDFUtils {

    public static final Log log = LogFactory.getLog(ZippedKeynoteToPDFUtils.class);

    protected static final int kSTATUS_UNKNOWN = 0;
    protected static final int kSTATUS_KEYNOTE = 1;
    protected static final int kSTATUS_NOT_KEYNOTE = -1;

    Blob blob = null;
    int status = kSTATUS_UNKNOWN;

    public ZippedKeynoteToPDFUtils(Blob inBlob) {
        blob = inBlob;
    }

    public boolean isZippedKeynote() throws Exception {

        if(status != kSTATUS_UNKNOWN) {
            return status == kSTATUS_KEYNOTE;
        }

        status = kSTATUS_NOT_KEYNOTE;
        if( blob != null
         && blob.getMimeType().equalsIgnoreCase("application/zip")) {
            ZipInputStream zis = new ZipInputStream(blob.getStream());
            ZipEntry zentry = null;
            String name;

            /* We are reading from a stream, not a file, so we can't make sure
             * it reads the "first" file, and can't rely on the fact that the
             * "zip contains a Keynote file if one of the 2 first elements
             * is a .key file (second can be a __MACOS folder).
             * The workaround is to find an item which:
             *      -> Is a directory (a Keynote presentation is a directory)
             *      -> The name ends with .key/
             *      -> And it contains one single "/" (just in case the Keynote
             *         format contains sub-.key files. Never seen that, but who
             *         knows and it is easy to test, not impacting performance)
             */
            do {
                zentry = zis.getNextEntry();
                if(zentry != null) {
                    name = zentry.getName();
                    if(zentry.isDirectory()
                         && name.endsWith(".key/")
                         && name.indexOf('/') == (name.length() - 1)) {
                        status = kSTATUS_KEYNOTE;
                    }
                    zis.closeEntry();
                }
            } while(zentry != null && status == kSTATUS_NOT_KEYNOTE);
            zis.close();
        }

        return status == kSTATUS_KEYNOTE;
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
