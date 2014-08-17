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
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * We do something only:
 *      - If the document is mutable,
 *      - And for the events and the doc. type defined in the extension
 *        point (seeZippedKeynoteToPDFEventHandler.xml)
 *
 *   => We do not re-check these events/doctypes here.
 *      This allows contribution tot he same point, so a nuxeo developper
 *      can add custom document types to the handler for example, and/or
 *      remove the File doc. type from being handled, etc.
 *
 * We also assume the event we receive is a DocumentEventContext event.
 *
 * Last but not least, The default configuration installs this handler
 * for "File" and "documentCreated", "documentModified". So it will
 * be called for every document of type "File", no exception. See
 * the README file to learn how to change this behavior.
 *
 * @author Thibaud Arguillere
 */
public class ZippedKeynoteToPDFEventHandler implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {

        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        DocumentModel doc = docCtx.getSourceDocument();
        if (!doc.isImmutable()) {
            try {
                HandleZippedKeynoteInDocument.run(doc, event.getContext().getCoreSession());
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
    }
}
