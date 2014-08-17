/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.keynote.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.keynote.ZippedKeynoteToPDFUtils;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author Thibaud Arguillere
 */

@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class, CoreFeature.class})
@Deploy({
    "org.nuxeo.ecm.automation.core",
    "nuxeo-keynote"
})
public class ConvertZippedKeynoteToPDFTest {

    @Inject
    CoreSession coreSession;

    @Test
    public void testIsZippedKeynoteBlob() throws Exception {
        File zipFile;
        FileBlob zipFileBlob;
        ZippedKeynoteToPDFUtils zkn2pdf;

        zipFile = FileUtils.getResourceFileFromContext("a-presentation.zip");
        zipFileBlob = new FileBlob(zipFile);
        zipFileBlob.setMimeType("application/zip");
        zkn2pdf = new ZippedKeynoteToPDFUtils(zipFileBlob);
        Assert.assertTrue(zkn2pdf.isZippedKeynote());

        zipFile = FileUtils.getResourceFileFromContext("not-a-presentation.zip");
        zipFileBlob = new FileBlob(zipFile);
        zipFileBlob.setMimeType("application/zip");
        zkn2pdf = new ZippedKeynoteToPDFUtils(zipFileBlob);
        Assert.assertFalse(zkn2pdf.isZippedKeynote());
    }
}
