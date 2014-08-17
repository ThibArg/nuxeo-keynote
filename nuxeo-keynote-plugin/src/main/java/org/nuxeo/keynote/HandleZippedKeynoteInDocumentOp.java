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
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author Thibaud Arguillere
 */
@Operation(id=HandleZippedKeynoteInDocumentOp.ID, category=Constants.CAT_DOCUMENT, label="Handle zipped Keynote in document", description="<p>Check if <code>file:content</code> is a zip file. If yes, check if it contains a .key package. If yes, convert this Keynote presentation to pdf, stores the pdf in the <code>knpdf:content</code> field, and set the <code>ZippedKeynote</code> facet</p>")
public class HandleZippedKeynoteInDocumentOp {

    public static final String ID = "HandleZippedKeynoteInDocument";

    @Context
    protected CoreSession session;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) throws Exception {

        return HandleZippedKeynoteInDocument.run(input,  session);
    }

}
