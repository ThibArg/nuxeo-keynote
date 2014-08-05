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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * @author Thibaud Arguillere
 *
 * When blob etc. etc. => talk about the usage of the "ZippedKeynote" facet
 *      -> Mainly, to be used in a EL to show/hide a button for example,
 *
 * Explain the KeynoteAsPDF schema
 *
 * Direct access from a GET call:
 * http://{server}:{port}/nuxeo/nxfile/default/{docId}/knpdf:content/{fileName}
 * http://localhost:8080/nuxeo/nxfile/default/094f5cb2-99cc-46a3-970d-7a30507c0d67/knpdf:content/Nuxeo-DM.key.zip.pdf
 *      => download the file as Nuxeo-DM.key.zip.pdf
 * http://localhost:8080/nuxeo/nxfile/default/094f5cb2-99cc-46a3-970d-7a30507c0d67/knpdf:content/
 *      => Downloads a file named file(indice): file(1), then file(2), etc.
 *
 */
@Operation(id=HandleZippedKeynoteinDocument.ID, category=Constants.CAT_DOCUMENT, label="Handle zipped Keynote in document", description="<p>Check if <code>file:content</code> is a zip file. If yes, check if it contains a .key file. If yes, convert this Keynote presentation to pdf, and stores the pdf in the <code>knpdf:content</code> field.</p>")
public class HandleZippedKeynoteinDocument {

    public static final String ID = "HandleZippedKeynoteInDocument";

    public static final Log log = LogFactory.getLog(HandleZippedKeynoteinDocument.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Context
    AutomationService   as;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) throws Exception {

        boolean doIt;

        // We do nothing if we don't have the correct kind of document.
        // We could return an error, but we are more generic here,
        // avoiding an hassle to the caller (checking the schemas and
        // calling us only if the document has the requested schemas)
        // (could log something, maybe)
        if(     input.isImmutable()
            || !input.hasSchema("file")
            || !input.hasSchema(ZippedKeynoteToPDFConstants.SCHEMA)) {
            return input;
        }

        doIt = true;

        // Get the main blob
        Blob zippedKeynoteBlob = (Blob) input.getPropertyValue("file:content");
        if (zippedKeynoteBlob == null) {
            BlobHolder bh = input.getAdapter(BlobHolder.class);
            if (bh != null) {
                zippedKeynoteBlob = bh.getBlob();
            }
        }

        // Convert to pdf
        //
        // There are 2 main points to handle here:
        //    * We must handle the fact that this code could be called in a
        //      "Document modified" handler and avoid recursive and infinite
        //      calls to this event (because we modify the document).
        //
        //    * We want to optimize the thing so we don't re-convert an already
        //      converted zip. This is achieved by storing the md5 hash of the
        //      original zipped keynote (the one stored in file:content). Then
        //      we compare the stored value with the current one which tells us
        //      if a conversion must be done
        //
        // So, basically:
        //      -> If there is no blob and no "ZippedKeynote" facet, then it means
        //         we are ok, no need to continue
        //
        //      -> If there is blob, we check the md5 to decide if we need to
        //         convert it or not.
        //
        //      -> If the blob is empty, we must also clear the facet and the schema
        //
        Blob resultPdf = null;
        if(zippedKeynoteBlob == null) {
            if(!input.hasFacet(ZippedKeynoteToPDFConstants.FACET)) {
                doIt = false; //return input;
            }
        } else {
            // Check if we already have a pdf for this .zip
            String storedHash = (String) input.getPropertyValue(ZippedKeynoteToPDFConstants.XPATH_ZIP_HASH);
            if(storedHash != null && zippedKeynoteBlob.getDigest().equals(storedHash)) {
                doIt = false; //return input;
            }
        }

        if(doIt) {
            ZippedKeynoteToPDFUtils zkn2pdf = new ZippedKeynoteToPDFUtils(zippedKeynoteBlob);
            resultPdf = zkn2pdf.convert();

            if(resultPdf == null) {
                input.removeFacet(ZippedKeynoteToPDFConstants.FACET);
                input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_FILENAME, null);
                input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_CONTENT, null);
                // If resultPdf is null, but we have a main blob, it just means it is
                // not zipped-keynote (can be a zip with no Keynote inside, but also
                // any other binary: pdf, docx, ...
                if(zippedKeynoteBlob == null) {
                    input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_ZIP_HASH, null);
                } else {
                    input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_ZIP_HASH, zippedKeynoteBlob.getDigest());
                }
            } else {
                input.addFacet(ZippedKeynoteToPDFConstants.FACET);
                // Put this blob in the KeynoteAsPDF schema
                resultPdf.setFilename(zippedKeynoteBlob.getFilename() + ".pdf");
                resultPdf.setMimeType("application/pdf");
                input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_FILENAME, zippedKeynoteBlob.getFilename() + ".pdf");
                input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_CONTENT, (Serializable) resultPdf);
                input.setPropertyValue(ZippedKeynoteToPDFConstants.XPATH_ZIP_HASH, zippedKeynoteBlob.getDigest());
            }
            input = session.saveDocument(input);
        }

        return input;
    }

}
