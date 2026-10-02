# Local browser OCR assets

- Tesseract.js 6.0.1: https://registry.npmjs.org/tesseract.js/-/tesseract.js-6.0.1.tgz
- Tesseract.js-core 6.1.2: https://registry.npmjs.org/tesseract.js-core/-/tesseract.js-core-6.1.2.tgz
- Thai and English LSTM data: https://github.com/tesseract-ocr/tessdata_fast
- Upstream API: https://github.com/naptha/tesseract.js/blob/master/docs/api.md

All assets are served by this application. Slip images are processed in browser
memory only: no OCR API, upload endpoint, image cache or persistent storage.
The worker is terminated after each read/cancellation, and the file input is
cleared. For the preview and re-read controls only, a File reference and Blob URL
remain in memory while the payment modal is open. Removing the image or closing
the modal releases the reference and revokes the URL. No image is sent in payment
FormData or kept in persistent storage. The user's original file is not deleted
from their computer. The maximum image size is 10 MB.
OCR does not verify authenticity or confirm that money was received.
Only date, time and amount are extracted. Party details and transaction references
are not extracted. Payment entry forms no longer include a reference field;
previously recorded references are preserved in the database.

Retain the bundled Apache 2.0 license files when redistributing these assets.
