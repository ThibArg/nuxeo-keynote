/*	===== IMPORTANT ===== IMPORTANT ===== IMPORTANT =====
	Modules to install on your node server:
			npm install node-uuid
			npm install adm-zip
			npm install applescript
	=====================================================

	This server is quite simple: Receives a .zip, returns a .pdf
	It assumes the .zip is a keynote presentation. Unexpected behavior if it is not the case.

	Technically, here is how it works:
		Conversion (main request):
			Get the file
			Unzip it
			Call an AppleScript to tell Keynote to export it to PDF
			Send back the pdf
			(all this being done asychronously)

		Cleanup of the files every minute

	To allow handling several conversions at the same time in Keynote, we use UUID as names of the documents.

	curl example:
	curl --upload-file nuxeo-DM.key.zip http://127.0.0.1:1337 -H "X-File-Name:nuxeo-DM.key.zip" -o "nuxeo-DM.pdf"
*/
var kDEBUG = true;
var kAUTO_CLEANUP_TIMEOUT = 60000;
var kCONVERSION_FOLDER_PATH = "/Users/thibaud/Desktop/KeynoteToPDFConversions/";

var http = require('http');
var fs = require('fs');
var util = require('util');
var path = require("path");
var child_process = require('child_process');

var uuid = require('node-uuid');
var AdmZip = require('adm-zip');
var applescript = require("applescript");

var kAPPLE_SCRIPT_TEMPLATE = "";

var gHandledDocs = [];

/* Startup

	Cleanup folder
	Install cleaning loop
	create and start the server
*/
console.log("Cleaning up the extraction folder...");
cleanupExtractionFolder(true);
console.log("...done");
// Install the cleaning scheduler every kAUTO_CLEANUP_TIMEOUT
setTimeout(function() {
	cleanupExtractionFolder(false, kAUTO_CLEANUP_TIMEOUT);
}, kAUTO_CLEANUP_TIMEOUT);

http.createServer(function(request,response){
	var theUid, destFolder, originalFileName,
		destFile, destFileStream, infos;

	response.writeHead(200);

	theUid = uuid.v4();
	destFolder = kCONVERSION_FOLDER_PATH + theUid + "/";
	fs.mkdirSync(destFolder);

	originalFileName = request.headers['x-file-name'];
	if(typeof originalFileName !== "string") {
		originalFileName = ""
	}

	destFile = destFolder + theUid + ".zip";
	destFileStream = fs.createWriteStream(destFile);
	request.pipe(destFileStream);

	infos = {	uid: theUid,
				folderPath: destFolder,
				timeStamp: Date.now(),
				done: false,
				originalFileName: originalFileName,
				pathToFileToHandle: destFile
			};
	gHandledDocs[theUid] = infos;
 
 /*
	var fileSize = request.headers['content-length'];
	var uploadedBytes = 0 ;
	request.on('data',function(d){
 
		uploadedBytes += d.length;
		var p = (uploadedBytes/fileSize) * 100;
		response.write("Uploading " + parseInt(p)+ " %\n");
	});
 */
	request.on('end',function(){
		//response.end("\nFile Upload Complete\n" + destFile);
		doUnzipConvertAndReturnPDF(infos, response);
	});
 
}).listen(1337,function(){
	console.log("node server started, listening on port 1337");
});

/*	doUnzipConvertAndReturnPDF

*/
function doUnzipConvertAndReturnPDF(infos, response) {

	var pathToExtractionFolder, zip, oldPath, newPath;

	logIfDebug("====> doUnzip");

	pathToExtractionFolder = appendSlashIfNeeded( infos.folderPath );
	zip = new AdmZip(infos.pathToFileToHandle);
	// Notice: extractAllTo() is synchronous
	zip.extractAllTo(pathToExtractionFolder, /*overwrite*/true);

	fs.readdir(pathToExtractionFolder, function(err, files) {
		var keynoteFileName = "";
		files.some(function(fileName) {
			if(stringEndsWith(fileName, ".key")) {
				keynoteFileName = fileName;
				return true;
			}
			return false;
		});
		if(keynoteFileName !== "") {
			// To handle the fact that several requests could ask to convert
			// documents with the same name, and to avoid conflicts in
			// Keynote, we use the UUID as name of the document
			oldPath = pathToExtractionFolder + keynoteFileName;
			newPath = pathToExtractionFolder + infos.uid + ".key";
			fs.renameSync(oldPath, newPath);
			infos.pathToFileToHandle = newPath;
			doConvertAndReturnPDF(infos, response);
		} else {
			console.log("Error: Can't find the .key file in the unzipped document");
    		returnTextResponse(500, "Error: Can't find the .key file in the unzipped document", response);
		}
	});
}

/*	doConvertAndReturnPDF

*/
function doConvertAndReturnPDF(infos, response) {
	logIfDebug("====> doConvertAndReturnPDF");
	
	var script;

	if(kAPPLE_SCRIPT_TEMPLATE == "") {
		kAPPLE_SCRIPT_TEMPLATE = 'tell application "Keynote"\n'
					+ '\n'
					+ '--if playing is true then tell the front document to stop\n'
					+ '\n'
					+ 'set conversionFolderPath to "<FOLDER_PATH/>"\n'
					+ '-- Open the presentation\n'
					+ 'set pathToKey to "<KEYNOTE_FILE_PATH/>"\n'
					+ 'open (POSIX file pathToKey) as alias\n'
					+ '\n'
					+ '-- Save a reference to this document\n'
					+ 'set thePresentation to document "<KEYNOTE_FILE_NAME/>"\n'
					+ '\n'
					+ '-- Set up names and paths\n'
					+ 'set documentName to the (name of thePresentation) & ".pdf"\n'
					+ 'set the targetFileHFSPath to ((POSIX file conversionFolderPath as alias) & documentName) as string\n'
					+ '\n'
					+ '-- Convert to pdf\n'
					+ 'export thePresentation to file targetFileHFSPath as PDF with properties {compression factor:0.3, export style:IndividualSlides, all stages:true, skipped slides:false}\n'
					+ '\n'
					+ '-- Done\n'
					+ 'close thePresentation\n'
					+ '\n'
					+ 'return the POSIX path of targetFileHFSPath\n'
					+ '\n'
					+ 'end tell\n';
	}

	script = kAPPLE_SCRIPT_TEMPLATE.replace("<FOLDER_PATH/>", infos.folderPath)
								   .replace("<KEYNOTE_FILE_PATH/>", infos.pathToFileToHandle)
								   .replace("<KEYNOTE_FILE_NAME/>", path.basename(infos.pathToFileToHandle) /*infos.uid + ".key"*/);
	//logIfDebug("-----------\n" + script + "\n----------");
	
	// We wait until the file is really here and valid
	waitUntilFileExists(infos.pathToFileToHandle, 25, 40, function(result) {
		if(result) {
			applescript.execString(script, function(err, result) {
				if(err) {
					console.log("Conversion error: " + err);
		    		returnTextResponse(500, "Conversion error:\n" + err, response);
				} else {
					infos.pathToFileToHandle = result;
					doReturnThePDF(infos, response);
				}
			});
		} else {
			console.log("Can't file the keynote file at "+ inKeynoteFileName);
			returnTextResponse(500, "Can't file the keynote file at "+ inKeynoteFileName, response);
		}
	});
}

/*	doReturnThePDF

*/
function doReturnThePDF(infos, response) {

	logIfDebug("====> doReturnThePDF");

	fs.readFile(infos.pathToFileToHandle, function(error, content) {
		if (error) {
			console.log("Error reading the pdf:\n" + error);
		    returnTextResponse(500, "Error reading the pdf:\n" + error, response);
		}
		else {
			response.writeHead(200, { 'Content-Type': 'application/pdf',
										'Content-Disposition': 'attachment; filename="' + infos.originalFileName + '"'
									});
			response.end(content, 'utf-8');
			infos.done = true;
		}
	});
}


/*	============================================================
	Utilities
	============================================================ */
/* logIfDebug

*/
function logIfDebug(inWhat) {
	if(kDEBUG) {
		console.log(inWhat);
	}
}
/* Cleanup

*/
function cleanupExtractionFolder(cleanAll, inNextTimeout) {

	var now, objs;

	cleanAll = cleanAll || false;

	if(cleanAll) {
		deleteFolderRecursiveSync(kCONVERSION_FOLDER_PATH);
	} else {
		now = Date.now();

		objs = Object.keys(gHandledDocs);
		if(objs.length > 0) {
			logIfDebug("Cleanup. " + objs.length + " folder"+ (objs.length > 1 ? "s" : "") + " to handle");
			Object.keys(gHandledDocs).forEach(function(key) {
				var obj = gHandledDocs[key];
				if(obj && (obj.done || (now - obj.timeStamp) > 30000))  {
					deleteFolderRecursiveSync(obj.folderPath);
					delete  gHandledDocs[key];
				}
			});
		}
	}
	// Schedule next iteration
	if(typeof inNextTimeout === "number" && inNextTimeout > 0) {
		setTimeout(function() {
			cleanupExtractionFolder(false, inNextTimeout);
		}, inNextTimeout);
	}
}

/*	returnTextResponse

*/
function returnTextResponse(inCode, inStrMessage, inResponseObj) {
	inResponseObj.writeHead(inCode, {'Content-Type': 'text/plain'});
	inResponseObj.end(inStrMessage);
}

/*	waitUntilFileExists

*/
function waitUntilFileExists(inPath, inTimeout, inMaxChecks, inCallback) {
	if(inMaxChecks <= 0) {
		inCallback(false);
	} else {
		if (fs.existsSync(inPath)) {
			inCallback(true);
		} else {
			setTimeout( function() {
				inMaxChecks -= 1;
				waitUntilFileExists(inPath, inTimeout, inMaxChecks, inCallback);
			}, inTimeout);
		} 
	}
}

/*	appendSlashIfNeeded

*/
function appendSlashIfNeeded(inPath) {
	if(typeof inPath === "string") {
		if(inPath.length === 0) {
			return "/";
		} else if(inPath[ inPath.length - 1 ] !== "/") {
			return inPath + "/";
		}
	}
	
	return inPath;
}

/*	deleteFolderRecursiveSync

	Thanks to http://www.geedew.com/2012/10/24/remove-a-directory-that-is-not-empty-in-nodejs/
*/
function deleteFolderRecursiveSync(path) {
  if( fs.existsSync(path) ) {
    fs.readdirSync(path).forEach(function(file,index){
      var curPath = path + "/" + file;
      if(fs.lstatSync(curPath).isDirectory()) { // recurse
        deleteFolderRecursiveSync(curPath);
      } else { // delete file
        fs.unlinkSync(curPath);
      }
    });
    if(path !== kCONVERSION_FOLDER_PATH) {
	    fs.rmdirSync(path);
	}
  }
};

/*	stringEndsWith

*/
function stringEndsWith(inStr, inToTest) {
	var position = inStr.length;
	position -= inToTest.length;
	var lastIndex = inStr.indexOf(inToTest, position);
	return lastIndex !== -1 && lastIndex === position;
}
// -- EOF--