nuxeo-keynote-plugin
====================

This [nuxeo](http://nuxeo.com) plug-in handles a zipped Keynote file, converts it to PDF, adds preview with pdf.js.

* [Build](#build)
* [Main Principles](#main-principles)
* [Using the Plug-in: What You Have to Do](#using-the-plug-in-what-you-have-to-do)
* [Customization](#customization)
* [Handling Custom Document Types](#handling-custom-document-types)
* [Trouble Shooting](#trouble-shooting)
* [License](#license)
* [About Nuxeo](#about-nuxeo)

## Build

See the README file of `nuxeo-keynote`.

## Main Principles

The plug-in provides tools, operations and a tab to handle the conversion (to PDF) of a zipped Keynote presentation, and the display of the resulting pdf. It also expects an external server to be running and ready to handle the actual conversion (from zipped Keynote to PDF). So, the main principles are:

* A server is up and running "somewhere":
  * On a Mac
  * With Keynote installed
  * We use a nodejs server for this purpose (see the "nuxeo-keynote-nodejs-server.js" file)
* The plug-in:
  * _(see the xml contributions)_
  * Declares/install some elements:
    * A `ZippedKeynote` facet
    * A `KeynoteAsPDF` schema (prefix `knpdf`), which holds, among other fields, a blob (the pdf)
      * This schema is added to the `File`  document type
    * A `zippedKeynoteToPDF` command line tool which uses `curl` to connect to the conversion server
    * A `zippedKeynoteToPDF` converter which uses this command line
  * Install an event handler
    * For `documentCreated` and `documentModified`,
    * And only for `File` documents
    * This handler does the same thing as the `HandleZippedKeynoteInDocument` operation (see below)
  * Install a `Keynote Preview` tab, displayed only if the current document has the `ZippedKeynote` facet
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
* Optimizations:
  * Native caching by the converter service: This avoids the exact same file will not be converted twice in the same session.
  * A specific field in the `KeynoteAsPDF` lets the plug-in to detect if the zip has already been handled, to avoid unzipping it, searching if it is a zipped keynote presentation

## Using the Plug-in: What You Have to Do
Basically nothing special unless you want to/have to:
* Use custom document type(s) instead of just `File`
* Build your own tab using the pdf.js preview, but also other widgets

See below how to disable the "Keynote Preview" tab, disable the event handler, use the preview widget, handle your own document types, handle your own event handler(s).

### Set up the NodeJS Conversion Server
* Make sure you have the node.js conversion server up and running on a Mac with Keynote and that your nuxeo server can access to it.
  * See explanations and code of the nodejs server [here](https://github.com/ThibArg/node-js-keynote2pdf)
* Set up `nuxeo.conf` so it contains the node.je server address:port. Something like:

  ```
  keynote2pdf.nodejs.server.urlAndPort=http://123.45.67.89:1234
  ```

### Customization
**IMPORTANT**: We are explaining how to configure your project using Nuxeo Studio.

#### 

#### Handle Custom Document Types
In Studio:
* Define your document type
  * Remember it must have the `file` _and_ the `KeynoteAsPDF` schemas
  * The easiest way to achieve this to extends the `File` document. Because the plug-in automatically adds this `KeynoteAsPDF` schema fo the `File` document type, extending this type will make everyting ok.
  * If you don't extend `File`, then you must add the `KeynoteAsPDF` schema to the Studio registry and then use this schema in the document type definition. See "Handling Custom Document Types" below.
* You must now use the plug-in to check the content, see if it is a .zip, see if it is a zipped keynote, etc.. You can achieve this in 2 different ways:
  * Override (contribute) the default listener as declared by the plug-in
  * Handle everything manually in an event handler. This may be useful if, for example, you have some specific things to do when the document is crated/modified

Let's explain both ways of doing it:
* **Contribute the event handler in an XML extension**
  * Create a new ["XML Extension"](http://doc.nuxeo.com/display/NXDOC/Contributing+to+an+Extension+Using+Nuxeo+Studio)
  * Name it, say, "contributeKn2PdfEventHandler"
  * Here is the XML to use. Just add you document types to the `<filters>` part:

``` xml
<require>org.nuxeo.keynote.listener.contrib.ZippedKeynoteToPDFEventHandler</require>
<extension target="org.nuxeo.ecm.core.event.EventServiceComponent"
           point="listener">
  <listener name="zippedkeynotetopdfeventhandler" async="true" postCommit="true"
      class="org.nuxeo.keynote.ZippedKeynoteToPDFEventHandler" order="100">
    <event>documentCreated</event>
    <event>documentModified</event>
    <filters>
      <doctype>File</doctype>
      <doctype>MyCustomDocType</doctype>
    </filters>
  </listener>
</extension>
```

* **Handle Everything Manually in Studio**
  * Disable the defaut handler:
    * Create a new ["XML Extension"](http://doc.nuxeo.com/display/NXDOC/Contributing+to+an+Extension+Using+Nuxeo+Studio)
    * Name it, for example "disableDefaultKn2PdfEventHandler"
    * You need to add `enabled="false"` to the listener. Here is the full XML to add in Studio:

``` xml
<require>org.nuxeo.keynote.listener.contrib.ZippedKeynoteToPDFEventHandler</require>
<extension
  target="org.nuxeo.ecm.core.event.EventServiceComponent"
  point="listener">
  <listener
      name="zippedkeynotetopdfeventhandler"
      enabled="false">
  </listener>
</extension>
```

  * Install an Event Handler(See [Studio documentation](http://doc.nuxeo.com/display/Studio/Event+Handlers))
    * For the `Document Created` and `Document Modified` events
    * Check the `Asynchronous` box. This is recommended because the conversion can take time (huge presentation for example) and we don't want to lock the user.
    * Only if the document is `Mutable Document`
    * And for any document having the `KeynoteAsPDF` schema
      * This way, you don't have to manually select `File`, `MyCustomDocType`, etc.
      * Add `#{currentDocument.hasSchema("KeynoteAsPDF")} to the `Custom EL expression` field
    * Then select the chain to run, for example `ZippedKeynote_onDocCreatedModified`

  * This `ZippedKeynote_onDocCreatedModified` chain is quite simple, it just calls the `HandleZippedKeynoteInDocument` operation:
  ```
    Fetch > Context Document(s)
    Document > Handle zipped Keynote in document
  ```
  But you could add here any logic you wish. Just remember the chain is executed asynchrously, so user will not see the changes immediately.

#### Dipplay a Custom Preview Tab
Here, we want to display a tab:
* Whose label is "Keynote" (not "Keynote Preview"
* Which contains the keynote preview
* Is displayed only if the current document has something to be displayed.
* And also contains other widgets (whatever: dublincore, lifecycle state, ...)

There are mainly two things to do:
* Create your tab and use the `pdf_using_pdfjs.xhtml` widget
* Disable the default tab displayed by the plug-in

So:
* Create a new Tab (see [Studio documentation](http://doc.nuxeo.com/display/Studio/Tabs))
  * In the "Definition" tab:
    * Set the label to "Keynote", and the order to 200 (so it is the last tab)
    * Click the "Add Row" button, and select a row which fits your need. The single element (top right of the dialog), or any other. Just give enough width to the preview
    * Drop a "Template" widget ("More Widgets" -> "Advanced Widgets") on this row
    * In the "Layout Widget Editor" dialog:
      * Hide the label
      * Click "Custom Properties Configuration"
        * (This adds a new property)
        * Set the key to `template`
        * Set the value to the name of the widget provided by the plug-in: "pdf_using_pdfjs.xhtml"
    * Now, add other rows, other widgets, etc.
  * In the "Enablement" tab
    * We want to display this tab only if current document has pdf containing a Keynote presentation
    * So, we fill the "Custom EL Expression" with this expression: `#{currentDocument.hasFacet("ZippedKeynote")}`      
  * (save)
* Disable the "Keynote Preview" tab provided by the plug-in
  * Create a new ["XML Extension"](http://doc.nuxeo.com/display/NXDOC/Contributing+to+an+Extension+Using+Nuxeo+Studio)
  * Name it for example "hideNativeKeynotePreviewTab"
  * in the xml we must (1) `require` the component (so we are sure to override it) and (2) disable the action (reminder: Any UI element in nueo is called an _action_). So the XML is:

``` xml
<require>org.nuxeo.keynote.preview</require>
<extension
  target="org.nuxeo.ecm.platform.actions.ActionService"
  point="actions">
  <action id="keynote_Preview" enabled="false"></action>
</extension>
```

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
