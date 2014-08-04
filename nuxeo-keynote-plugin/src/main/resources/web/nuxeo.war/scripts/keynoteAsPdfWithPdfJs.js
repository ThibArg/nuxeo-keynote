/*	keynoteAsPdfWithPdfJs.js */

// Encapsulating everything to avoid collision with outside variable names
// (for example, ctx is used elsewhere in the browser)
function displayPDF(inDocId, inPdfWorkerPath, inCanvasId) {

	var url,
		pdfDoc = null,
		pageNum = 1,
		pageRendering = false,
		pageNumPending = null,
		countOfPages = 0,
		scale = 0.8,
		canvas = null,
		ctx = null,
		First = null,
		bPrev,
		bNext,
		bLast;

	// Setup the worker
	PDFJS.workerSrc = inPdfWorkerPath;

	// Update the buttons
	document.getElementById('knpdf_first').addEventListener('click', onFirstPage);
	document.getElementById('knpdf_prev').addEventListener('click', onPrevPage);
	document.getElementById('knpdf_next').addEventListener('click', onNextPage);
	document.getElementById('knpdf_last').addEventListener('click', onLastPage);

	// Setup the url to the pdf
	url = "/nuxeo/nxfile/default/" + inDocId + "/knpdf:content/";

	//PDFJS.disableWorker = true;
	//debugger;

	// Render the pdf
	canvas = document.getElementById(inCanvasId);
	ctx = canvas.getContext('2d');
	PDFJS.getDocument(url).then(function(pdfDoc_) {
		pdfDoc = pdfDoc_;
		countOfPages = pdfDoc.numPages;
		document.getElementById('knpdf_page_count').textContent = countOfPages;

		// Initial/first page rendering
		renderPage(pageNum);
	});

	// ================================== sub routines
	// ===============================================
	function renderPage(num) {
		pageRendering = true;
		// Using promise to fetch the page
		pdfDoc.getPage(num).then(function(page) {
			var viewport = page.getViewport(scale);
			canvas.height = viewport.height;
			canvas.width = viewport.width;

			// Render PDF page into canvas context
			var renderContext = {
					canvasContext: ctx,
					viewport: viewport
			};
			var renderTask = page.render(renderContext);

			// Wait for rendering to finish
			renderTask.promise.then(function() {
				pageRendering = false;
				if (pageNumPending !== null) {
					// New page rendering is pending
					renderPage(pageNumPending);
					pageNumPending = null;
				}
				updateUI();
			});
		});

		// Update page counters
		document.getElementById('page_num').textContent = pageNum;
	}


	function queueRenderPage(num) {
		if (pageRendering) {
			pageNumPending = num;
			updateUI();
		} else {
			renderPage(num);
		}
	}

	function updateUI() {
		if (bFirst == null) {
			bFirst = jQuery("#knpdf_first");
			bPrev = jQuery("#knpdf_prev");
			bNext = jQuery("#knpdf_next");
			bLast = jQuery("#knpdf_last");
		}

		bFirst.attr("disabled", pageNum < 2);
		bPrev.attr("disabled", pageNum < 2);
		bNext.attr("disabled", pageNum >= countOfPages);
		bLast.attr("disabled", pageNum >= countOfPages);

	}

	function onFirstPage() {
		pageNum = 1;
		queueRenderPage(1);
	}

	function onPrevPage() {
		if (pageNum <= 1) {
			return;
		}
		pageNum--;
		queueRenderPage(pageNum);
	}

	function onNextPage() {
		if (pageNum >= pdfDoc.numPages) {
			return;
		}
		pageNum++;
		queueRenderPage(pageNum);
	}

	function onLastPage() {
		pageNum = countOfPages;
		queueRenderPage(pageNum);
	}

} // displayPDF
//--EOF--


