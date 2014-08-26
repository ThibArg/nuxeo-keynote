/*	nuxeo-keynote-nodejs-server.js

	Code for the nodejs server receiving a request, handling it and returning the
	pdf to the client.

	This code is part of the nuxeo-keynote plug-in for Nuxeo, but it actually could
	be used in any other context, it does not depend on Nuxeo.

	--------------------------------------------------
	IMPORTANT: DEPENDENCIES
	--------------------------------------------------
	The following modules must be installed on your server:
			npm install keynote2pdf
			npm install node-uuid

	Also, keynote2pdf depends on (see its documentation):
			npm install node-uuid
			npm install adm-zip
			npm install applescript

	Reminder:
		(1) This server *must* run on Mac
	and (2) the Mac *must* have Keynote installed.

	--------------------------------------------------
	USAGE
	--------------------------------------------------
	* On you Nuxeo server, set up the keynote2pdf.nodejs.server.urlAndPort
	  parameter with the server:port of your nodejs server

	* Have a Mac with Keynote, nodejs and npm installed

	* Install the 3d party modules
		npm install keynote2pdf

	  Because keynote2pdf declares its dependencies, you should have node-uuid,
	  adm-zip and applescript. Anyway, if you don't have them, node will let
	  you know as soon as you start it with this script :-)

	* To avoid hard-coding the server, port and parameters in the code, you *must*
	  have a nuxeo-keynote-nodejs-server-config.json file containing JSON code
	  with the parameters:
	  		server
	  		port
	  		cleanup_timeout OPTIONNAL
	  		max_lifespan	OPTIONNAL
	  		debug			OPTIONNAL
	  Note: cleanup_timeout, max_lifespan and debug are passed to keynote2pdf
	  Note #2: Not having this file or not having the server and port properties
	           defined will forbig the server to start.


	* This server handles 2 requests:
		/just_checking
			As its name state. Returns 200 and "All is good" if, well, all is good

		/convert/ and the zip file

	* That's all. The nuxeo-keynote plug-in does a curl call to get the pdf:
		curl --upload-file #{sourceFilePath} #{nodeServerUrl} -o #{targetFilePath}

	* NOTE: We receive the zip files in a temp. folder, and try to clean each of
	  them as soon as they are no more needed
*/
// Core modules
var http = require("http");
var fs = require("fs");
var os = require("os");
var util = require("util");

// Third party modules
var keynote2pdf = require("keynote2pdf");
var uuid = require("node-uuid");

// Constants
const kCONFIG_FILE_PATH = __dirname + '/nuxeo-keynote-nodejs-server-config.json';

// Declare the temporary folder where will store the zips
var gDestTempFolder = os.tmpDir() + "nuxeo-keynote-received-zips/";
if(!fs.existsSync(gDestTempFolder)) {
	fs.mkdirSync(gDestTempFolder);
}

// -> Get the configuration
var gConfig = null;
console.log("Read configuration...");
if(fs.existsSync(kCONFIG_FILE_PATH)) {
	var configData = fs.readFileSync(kCONFIG_FILE_PATH, {encoding:'utf8'});
	gConfig = JSON.parse(configData);
}
if(gConfig == null) {
	console.log("...ERROR - No configuration file found, can't start the server");
	return;
}
console.log("...Configuration: \n" + util.inspect(gConfig) + "\n");

// Prepare the keynote2pdf config
var gKn2pdfConfig = {};
if("cleanup_timeout" in gConfig) {
	gKn2pdfConfig.cleanup_timeout = gConfig.cleanup_timeout;
}
if("max_lifespan" in gConfig) {
	gKn2pdfConfig.max_lifespan = gConfig.max_lifespan;
}
if("debug" in gConfig) {
	gKn2pdfConfig.debug = gConfig.debug;
}
keynote2pdf.initSync(gKn2pdfConfig);

// -> Create and start the server
console.log("Start server...");
http.createServer(function(request, response) {
	var theUid,
		destZipFile,
		destFileStream,
		doConvert = false;

console.log(request.url);

	// Dispatch the request
	if(request.url === "/just_checking") {
		// Nothing more. We're alive
		console.log("just_checking -> We're alive, aren't we?");
		response.writeHead(200, {'Content-Type': 'text/plain'});
		response.end("All is good");
	} else if(request.url.indexOf("/convert/") === 0){
		doConvert = true;
	} else {
		// We reject any other requests with as few info as possible
		response.writeHead(200);
		response.end();
	}

	if(doConvert) {
	// Get the file sent by the client, save it in the temp folder with a unique name
		destZipFile = gDestTempFolder + uuid.v4() + ".zip";
		destFileStream = fs.createWriteStream(destZipFile);
		request.pipe(destFileStream);
		
	// Once the file is received, convert it
		request.on('end',function(){
			keynote2pdf.convert(destZipFile, function(inError, inData) {
				var readStream;
		// ---------------------------------------------------------------
		// If we have an error, stop everything
		// ---------------------------------------------------------------
				if(inError) {
					console.log("Got an error: " + inError);
					response.writeHead(500, {'Content-Type': 'text/plain'});
					response.end("Got an error: " + inError);
					// Delete our zip
					fs.unlinkSync(destZipFile);
				} else {
		// ---------------------------------------------------------------
		// If we have no error, then return the pdf if we have it
		// ---------------------------------------------------------------
					if(inData.step === keynote2pdf.k.STEP_DONE) {
						readStream = fs.createReadStream(inData.pdf);
					// When the stream is ready, send the data to the client
						readStream.on('open', function () {
						// This pipe() API is amazing ;-)
							readStream.pipe(response);
						});
					// When all is done with no problem, do some cleanup
						readStream.on('end', function () {
						// Tell keynote2pdf the temporary files used for this conversion
						// can be cleaned up
							keynote2pdf.canClean(inData.uid);
						// Do our own cleanup and delete the zip
							fs.unlinkSync(destZipFile);
						});
					// In case of error, also do some cleanup
						readStream.on('error', function (inErr) {
							console.log("readStream -> error: " + inErr);
							response.end(inError);
							keynote2pdf.canClean(inData.uid);
							fs.unlinkSync(destZipFile);
						});
					// We don't use this one
						/*
						readStream.on('data', function (chunk) {
							console.log('got %d bytes of data', chunk.length);
						});
						*/
					}
				}
			}); // keynote2pdf.convert
		}); // request.on('end'...)
	}
}).listen(gConfig.port, gConfig.server, function(){
	console.log("...node server started, listening on " + gConfig.server + ":" + gConfig.port);
});

// -- EOF--