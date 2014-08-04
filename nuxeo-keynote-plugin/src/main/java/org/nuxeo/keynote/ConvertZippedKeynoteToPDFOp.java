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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author Thibaud Arguillere
 */
@Operation(id=ConvertZippedKeynoteToPDFOp.ID, category=Constants.CAT_CONVERSION, label="Convert Zipped Keynote to PDF", description="<p>This operation receives a blob which a zip file containing a Keynote presentaiton (<code>.key</code> extension). It returns a pdf after conversion by Keynote.</p>")
public class ConvertZippedKeynoteToPDFOp {

    public static final String ID = "ConvertZippedKeynoteToPDF";

    @OperationMethod(collector=BlobCollector.class)
    public Blob run(Blob input) throws Exception {

        ZippedKeynoteToPDFUtils zkn2pdf = new ZippedKeynoteToPDFUtils(input);
        Blob resultPdf = zkn2pdf.convert();

        return resultPdf;
    }

}
