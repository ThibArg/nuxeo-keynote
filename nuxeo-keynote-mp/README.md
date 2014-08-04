nuxeo-cm-demo-utils-mp
======================

This part creates the Nuxeo Marketplace Package for nuxeo-cm-demo-utils.

## Build

_Note_: You can also get the .zip of the package in the "Releases" part of this project (on GitHub)

Assuming maven is correctly setup on your computer:

1. Build the plug-in first. Switch to the `nuxeo-cm-demo-utils` project:, and build the plug-in. Use "install" to install it in the local maven repository
    ```
    cd /path/to/nuxeo-cm-demo-utils-plugin
    mvn install
    # The plug-in is in /target, named nuxeo-cm-demo-utils-{version}.jar
    ```


2. Build the MarketPlace Package. Switch to the `nuxeo-cm-demo-utils-mp` project, then:
    ```
    cd /path/to/nuxeo-cm-demo-utils-mp
    mvn package
    # The MP is in target, named nuxeo-cm-demo-utils-{version}.zip
    ```

**WARNING**: If building the Marketplace Package fails, you probably did not use `install` when compiling the plug-in with maven, or an error occured.
