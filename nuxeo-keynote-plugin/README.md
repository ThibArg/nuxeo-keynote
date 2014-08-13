nuxeo-keynote-plugin
====================

This [nuxeo](http://nuxeo.com) plug-in handles a zipped Keynote file, converts it to PDF, adds preview with pdf.js.

* [Build](#build)
* [Main Principles](#main-principles)
* [Using the Plug-in: What You Have to Do](#using-the-plug-in-what-you-have-to-do)
* [Handling Custom Document Types](#handling-custom-document-types)
* [Trouble Shooting](#trouble-shooting)
* [License](#license)
* [About Nuxeo](#about-nuxeo)

## Build

See the README file of `nuxeo-keynote`.

## Main Principles

The plug-in provides tools, operations and a widget to handle the conversion (to PDF) of a zipped Keynote presentation, and the display of the resulting pdf. It also expects an external server to be running and ready to handle the actual conversion (from zipped Keynote to PDF). So, the main principles are:

* A server is up and running "somewhere" on a Mac with Keynote installed
* The plug-in
  * Declares/install some elements (see `zippedKeynote2PDF-contrib.xml`)':
    * A `ZippedKeynote` facet
    * A `KeynoteAsPDF` schema (prefix `knpdf`), which holds, among other fields, a blob (the pdf)
      * This schema is added to the `File`  document type
    * A `zippedKeynoteToPDF` command line tool which uses `curl` to connect to the conversion server
    * A `zippedKeynoteToPDF` converter which uses this command line
  * Provides two operations:
    * `ConvertZippedKeynoteToPDF` just receives a blob, calls the service for conversion, returns the pdf
    * `HandleZippedKeynoteInDocument` receives a `Document` and:
      * Checks its `file:content` blob
      * If it is a zip *and* this zip contains a Keynote presentation, then:
        * Calls the conversion server for conversion to pdf
        * Adds the `ZippedKeynote` facet to the document
        * Fills the `KeynoteAsPDF` schema
      * If it not a zip, or if the zip does not contain a Keynote presentation, or there is no blob, etc., then the plug-in:
        * Removes the `ZippedKeynote` facet (or does nothing if the document did not have this facet)
        * Cleans up the `KeynoteAsPDF` schema
    * Provides the `pdf_using_pdfjs.xhtml` widget template which
      * Uses pdf.js
      * And displays the pdf stored in the `knpdf:content` field

## Using the Plug-in: What You Have to Do
The plug-in provides operations, but no user action or event handler to call these operations. Also, the plug-in provides the `pdf_using_pdfjs.xhtml` widget but does not use it (in a tab or a dialog, ...). All this is cool ;-) because it gives you more control over the behavior.

### Set up the NodeJS Conversion Server
* Make sure you have the node.js conversion server up and running on a Mac with Keynote and that your nuxeo server can access to it.
  * See explanations and code of the nodejs server [here](https://github.com/ThibArg/node-js-keynote2pdf)
* Set up `nuxeo.conf` so it contains the node.je server address:port, and possibly the token (if the nodejs server expects a token). Something like:

  ```
  keynote2pdf.nodejs.server.urlAndPort=http://123.45.67.89:1234
  # Optional. Check with the nodejs server configuration
  keynote2pdf.nodejs.server.token=0a1b2c3d4e-etc
  ```

### Set up your Project: An Example
**IMPORTANT**: We are explaining how to configure your project using Nuxeo Studio.

Say we want to handle zipped Keynote presentations stored in `File` documents (To handle other document types, see below "Handling Custom Document Types.) and display the preview in a Tab.

#### Handle the `File` Documents
In Studio:
* Install an Event Handler for `File` documents (See [Studio documentation](http://doc.nuxeo.com/display/Studio/Event+Handlers))
  * The plug-in adds the `KeynoteAsPDF` schema to the `File`document type.
  * So we create an event handler and set its properties to:
    * Be triggered for the `Document Created` and `Document Modified` events
      * Because we want to handle posible modifications of the presentation
    * Asynchronous
      * This is recommended because the conversion can takes time (huge presnetation for example)
    * Be available only for `File` documents, because we know they have the `KeynoteAsPDF` schema.
        * The plug-in handles gracefully calls for documents which don't have the `KeynoteAsPDF` schema (it just does nothing), but it is a good habit to make sure EventHandlers are triggered only when needed
    * Run a chain named, for example `File_onDocCreatedModified`

* This `File_onDocCreatedModified` chain is quite simple, it just calls the `HandleZippedKeynoteInDocument` operation:
  ```
    Fetch > Context Document(s)
    Document > Handle zipped Keynote in document
  ```

#### Dipplay a Preview in a Tab
Here, we want to display a tab whose lable is "Keynote Preview" and is displayed only if the current document has something to be displayed. So:

* Create a new Tab (see [Studio documentation](http://doc.nuxeo.com/display/Studio/Tabs))
* In the "Definition" tab:
  * Set the label to "Keynote Preview", and the order to 10,000 (so it is the last tab)
  * Click the "Add Row" button, and select the row which contains one single element (top right of the dialog)
  * Then, remove the first row (whiwh was created by default by Studio)
  * Drop a "Template" widget ("More Widgets" -> "Advanced Widgets")
  * In the "Layout Widget Editor" dialog:
    * Hide the label
    * Click "Custom Properties Configuration"
      * This adds a new property
      * Set the key to `template`
      * Set the value to the name of the widget provided by the plug-in: "pdf_using_pdfjs.xhtml"
* In the "Enablement" tab
  * We want to display this tab only if current document has pdf containing a Keynote presentation
  * So, we fill the "Custom EL Expression" with this expression: `#{currentDocument.hasFacet("ZippedKeynote")}`      
* (save)

#### Test
* Make sure the plug-in is deployed on your server, and update the server with your Studio project.
* Create a `File` document, or drag-drop a .zip containing a Keynote presentation
* If all went well, you will see the "Keynote Preview" tab
  * If you created the document using the "New" button, you will not see the "Keynote Preview" tabe, because the event is asynchronous and the UI is not refreshed. Just reload the page
* If you open/create/modify a `File` you will see that the tab is displayed only if the main file is a zip and this zip contains a Keynote presentation.

## Handling Custom Document Types
The plug-in installs its behavior for `File` documents. If you want/need to create custom document types which can contain zipped Keynote presentations then you can (in Studio):

* Add the `KeynoteAsPDF` schema to the Studio registry. Here is the schema for the registry:
```
{
  schemas: {
    KeynoteAsPDF: {
      @prefix: "knpdf",
      filename: "string",
      zipHash: "string",
      content: "blob"
    }
  }
}
```
* Add this schema to your documents (in the "Definition" tab)
* Update the EventHandler so it is also triggered for your document type

**IMPORTANT**: Your document must have the `file` schema, because the plug-in reads the .zip from this schema (or do nothing if the current document does not have this schema)

## Trouble Shooting

* Is your node.js server up and running?
  * Just do, from your browser, the "just_checking" request: `http://yourserver:theport/just_checking`

* Did you set-up the EventHandler(s) correctly?

* Did you set-up your custom document type correctly (the `KeynoteAsPDF` schema, the `file` schema)

## License
```
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
```

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>
