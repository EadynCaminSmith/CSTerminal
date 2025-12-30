/**
* Ipos Printer Service
* IPosPrinterService.aidl
* AIDL Version: 1.0.0
*/

package com.iposprinter.iposprinterservice;
import  com.iposprinter.iposprinterservice.IPosPrinterCallback;
import  android.graphics.Bitmap;

interface IPosPrinterService {
    /**
    * Printer status query
    * @return Current status of the printer
    * <ul>
    * <li>0:PRINTER_NORMAL Ready
    * <li>1:PRINTER_PAPERLESS Out of paper
    * <li>2:PRINTER_THP_HIGH_TEMPERATURE Overheated
    * <li>3:PRINTER_MOTOR_HIGH_TEMPERATURE Motor overheated
    * <li>4:PRINTER_IS_BUSY Busy
    * <li>5:PRINTE_ERROR_UNKNOWN Error
    */
    int getPrinterStatus();

    /**
    * Printer initialization
    * @param callback Execution result callback
    */
    void printerInit(in IPosPrinterCallback callback);

    /**
    * Set print density
    * @param depth: Density level 1-10
    * @param callback Execution result callback
    */
    void setPrinterPrintDepth(int depth,in IPosPrinterCallback callback);

    /**
    * Set print font type
    * @param typeface: Font name
    * @param callback Execution result callback
    */
    void setPrinterPrintFontType(String typeface,in IPosPrinterCallback callback);

	/**
	* Set print font size
	* @param fontsize: Font size
	* @param callback Execution result callback
	*/
    void setPrinterPrintFontSize(int fontsize,in IPosPrinterCallback callback);

    /**
    * Set print alignment
    * @param alignment: 0--Left , 1--Center, 2--Right
    * @param callback Execution result callback
    */
    void setPrinterPrintAlignment(int alignment, in IPosPrinterCallback callback);

    /**
    * Feed paper
    * @param lines: Number of lines
    * @param callback Execution result callback
    */
    void printerFeedLines(int lines,in IPosPrinterCallback callback);

    /**
    * Print blank lines
    * @param lines: Number of lines
    * @param height: Height of each line
    * @param callback Execution result callback
    */
    void printBlankLines(int lines,int height,in IPosPrinterCallback callback);

    /**
    * Print text
    * @param text: Text to print
    * @param callback Execution result callback
    */
    void printText(String text, in IPosPrinterCallback callback);

    /**
    * Print specified type text
    * @param text: Text to print
    * @param typeface: Font name
    * @param fontsize: Font size
    * @param callback Execution result callback
    */
    void printSpecifiedTypeText(String text, String typeface,int fontsize,in IPosPrinterCallback callback);

    /**
    * Print formatted text
    * @param text: Text to print
    * @param typeface: Font name
    * @param fontsize: Font size
    * @param alignment: 0-Left, 1-Center, 2-Right
    * @param callback Execution result callback
    */
    void PrintSpecFormatText(String text, String typeface, int fontsize, int alignment, IPosPrinterCallback callback);

	/**
	* Print column text
	* @param colsTextArr Column text array
	* @param colsWidthArr Column width array
	* @param colsAlign Column alignment array
	* @param isContinuousPrint 1-Continuous, 0-Single
	* @param callback Execution result callback
	*/
	void printColumnsText(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlign,int isContinuousPrint, in IPosPrinterCallback callback);

    /**
    * Print bitmap
    * @param alignment: 0--Left , 1--Center, 2--Right
    * @param bitmapSize: 1~16
    * @param mBitmap: Bitmap object
    * @param callback Execution result callback
    */
    void printBitmap(int alignment, int bitmapSize, in Bitmap mBitmap, in IPosPrinterCallback callback);

	/**
	* Print barcode
	* @param data: Data
	* @param symbology: Type
	* @param height: Height
	* @param width: Width
	* @param textposition: Position
	* @param callback Execution result callback
	*/
	void printBarCode(String data, int symbology, int height, int width, int textposition,  in IPosPrinterCallback callback);

	/**
	* Print QR code
	* @param data: Data
	* @param modulesize: Size
	* @param mErrorCorrectionLevel Error correction
	* @param callback Execution result callback
	*/
	void printQRCode(String data, int modulesize, int mErrorCorrectionLevel, in IPosPrinterCallback callback);

	/**
	* Print raw byte data
	* @param rawPrintData Byte array
	* @param callback Execution result callback
	*/
	void printRawData(in byte[] rawPrintData, in IPosPrinterCallback callback);

	/**
	* Send ESC/POS command
	* @param data Command
	* @param callback Execution result callback
	*/
	void sendUserCMDData(in byte[] data, in IPosPrinterCallback callback);

	/**
	* Perform print
	* @param feedlines: Feed paper lines
	* @param callback Execution result callback
	*/
	void printerPerformPrint(int feedlines, in IPosPrinterCallback callback);
}
