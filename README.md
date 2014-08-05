nuxeo-keynote
=============

Handles a zipped Keynote file, converts it to PDF, adds preview with pdf.js.

### Build

Assuming maven 3.2.1 is correctly setup on your computer:

    cd /path/to/nuxeo-keynote
    mvn install
    # The Marketplace Package is in nuxeo-keynote-mp/target, named nuxeo-keynote-mp-{version}.zip
    # The Plug-in itself is in nuxeo-keynote-plugin/target, named nuxeo-keynote-plugin-{version}.jar
    # (but using the MP makes things way easier for installation, de-installation server side)

### Details and Explanations
See the README file of `nuxeo-keynote-plugin`.

### License
(C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 
All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Thibaud Arguillere (https://github.com/ThibArg)

### About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>
