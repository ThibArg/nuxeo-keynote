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

public class ZippedKeynoteToPDFConstants {
    // WARNING: All these *MUST* match zippedKeynote2PDF.xml
    public static final String FACET = "ZippedKeynote";

    public static final String SCHEMA = "KeynoteAsPDF";

    public static final String SCHEMA_PREFIX = "knpdf";

    public static final String XPATH_FILENAME = SCHEMA_PREFIX + ":filename";
    public static final String XPATH_CONTENT = SCHEMA_PREFIX + ":content";
    public static final String XPATH_ZIP_HASH = SCHEMA_PREFIX + ":zipHash";

    // Configuraiton to be set in nuxeo.conf
    //      -> The node.js server:port address to be used with curl
    public static final String kKEYNOTE2PDF_NODEJS_SERVER_URL_KEYNAME = "keynote2pdf.nodejs.server.urlAndPort";
    //      -> Optional: A token, if the node.js server requires one
    public static final String KEYNOTE2PDF_NODEJS_SERVER_TOKEN_NAME= "keynote2pdf.nodejs.server.token";
}
