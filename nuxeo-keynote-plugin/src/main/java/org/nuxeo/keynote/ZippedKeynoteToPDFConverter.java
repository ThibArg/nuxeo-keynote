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
import java.util.Set;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.convert.plugins.PDF2ImageConverter;

/*  This class is called by the converter, as declared in the
 *  zippedKeynote2PDF-contrib.xml contribution
 */
public class ZippedKeynoteToPDFConverter extends PDF2ImageConverter {

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        // TODO Auto-generated method stub
        Map<String, String> cmdStringParameters= super.getCmdStringParameters(blobHolder, parameters);

        Map<String, String> stringParameters = new HashMap<String, String>();
        Set<String> parameterNames = parameters.keySet();
        for (String parameterName : parameterNames) {
            //targetFilePath is computed by the method of the superType
            if (!parameterName.equals("targetFilePath")) {
                stringParameters.put(parameterName,
                        (String) parameters.get(parameterName));
            }
        }
        cmdStringParameters.putAll(stringParameters);
        return cmdStringParameters;
    }

}
