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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 *
 * @author Thibaud Arguillere
 */
public class ZippedKeynoteToPDFEventHandler implements PostCommitEventListener {

    //public static final Log log = LogFactory.getLog(ZippedKeynoteToPDFEventHandler.class);

    /*
     * Because we run asynchronously, we must check the document. For example,
     * it could have been deleted after the event was pushed in the event stack
     * but before we are actually called.
     *
     * IMPORTANT: WE DON'T CHECK THE KIND OF EVENT, which by default can be
     * documentCreated or documentModified. This allows more flexibility if the
     * developer wants to override this and adds other events
     */
    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {
        for (Event theEvent : bundle) {
            if(theEvent.getContext() instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) theEvent.getContext();
                DocumentModel doc = docCtx.getSourceDocument();

                if(doc != null && !doc.isImmutable()) {
                    try {
                        HandleZippedKeynoteInDocument.run(doc, docCtx.getCoreSession());
                    } catch (Exception e) {
                        throw new ClientException(e);
                    }
                }
            }
        }
    }
}
