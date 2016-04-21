package tnefern.honeybeeframework.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import tnefern.honeybeeframework.stats.JobInfo;
import tnefern.honeybeeframework.stats.TimeMeter;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;
import dalvik.system.PathClassLoader;

/**
 * 
 * @author tnfernando
 */
public class FileFactory {

	private static FileFactory theInstance = null;
	private StringBuffer stealTracer = null;
	private int fileCount = 0;

	public static FileFactory getInstance() {
		if (theInstance != null) {
			return theInstance;
		} else {
			theInstance = new FileFactory();
			return theInstance;
		}
	}

	private FileFactory() {
		stealTracer = new StringBuffer();
	}

	public void writeFileWithDate(String pStr) throws IOException {
		StringBuffer s = new StringBuffer("\n"
				+ DateFormat.getDateTimeInstance().format(new Date()));
		s.append(pStr);

		writeFile(s.toString());

	}

	public void writeFileWithDate(String path, String pStr) throws IOException {
		StringBuffer s = new StringBuffer("\n"
				+ DateFormat.getDateTimeInstance().format(new Date()));
		s.append(pStr);
		writeFile(path, s.toString());

	}

	public void writeFile(String pStr) throws IOException {
		writeFile(CommonConstants.DEBUG_FILE_PATH, pStr);
	}

	public void writeFile(String path, String pStr) throws IOException {
		File sdDir = Environment.getExternalStorageDirectory();

		if (sdDir.canWrite()) {
			File txtfile = new File(sdDir, path);
			// FileWriter txwriter = new FileWriter(txtfile);
			// BufferedWriter out = new BufferedWriter(txwriter);
			// out.write(pStr);
			// out.close();

			FileOutputStream fOut = new FileOutputStream(txtfile, true);
			OutputStreamWriter osw = new OutputStreamWriter(fOut);
			osw.write(pStr);
			osw.write("\n");
			osw.flush();
			osw.close();

		}

		// FileOutputStream fOut = pAct.openFileOutput(path, pAct.MODE_APPEND);
		// OutputStreamWriter osw = new OutputStreamWriter(fOut);
		//
		// osw.write(pStr);
		// osw.flush();
		// osw.close();

	}

	/*
	 * public boolean writeFile(String path, String pStr, boolean pFlag, String
	 * pFirst) { FileConnection fc = null; OutputStream os = null;
	 * OutputStreamWriter w = null; long offset; boolean res = false; try { fc =
	 * (FileConnection) Connector.open(path); if (fc.exists()) { // file exists,
	 * open at EOF. offset = fc.fileSize(); os = fc.openOutputStream(offset); }
	 * else { // file does not exist, create and open. fc.create(); os =
	 * fc.openOutputStream(); } w = new OutputStreamWriter(os, "US-ASCII"); if
	 * (!pFlag) { w.write(pFirst); // pFlag = true; } w.write(pStr); w.close();
	 * fc.close(); res = true; } catch (Exception e) { e.printStackTrace(); }
	 * finally { try { if (null != w) { w.close(); } w = null; } catch
	 * (Exception e2) { e2.printStackTrace(); } try { if (null != fc) {
	 * fc.close(); } fc = null; } catch (Exception e3) { e3.printStackTrace(); }
	 * } return res; }
	 */

	public String[] tokenize(String pS, String pToken, int len) {
		String[] retSrr = new String[len];
		int i = 0;
		// Log.d("tokenize", "Original = "+pS);
		// Vector strVec = new Vector();
		// while (pS.length() > 1) {
		// int ind = pS.indexOf(pToken);
		// String st = pS.substring(0, ind);
		// System.out.println(st);
		// pS = pS.substring(ind + 1, pS.length());
		// strVec.addElement(st);
		// }
		// return strVec;
		// System.out.println(pS);
		while (true) {
			int ind = pS.indexOf(pToken);
			String st = pS.substring(0, ind);
			// strVec.addElement(st);
			retSrr[i] = st;
			// System.out.println("st = " + st);
			pS = pS.substring(ind + 1, pS.length());
			// System.out.println(s);
			if (pS.indexOf(pToken) < 0) {
				// Log.d("tokenize",
				// "at this point pS = "+pS+" , ind = "+ind+" retSrr.len = "+retSrr.length);
				st = pS;
				// strVec.addElement(st);
				retSrr[i + 1] = st;
				// System.out.println("st = " + st);
				break;
			}
			i++;
		}
		return retSrr;
	}

	// public String[] tokenize2(String pS, String pToken, int len) {
	// String[] retSrr = new String[len];
	// int i = 0;
	//
	// while (true) {
	// int ind = pS.indexOf(pToken);
	// String st = pS.substring(0, ind);
	// retSrr[i] = st;
	// System.out.println("st = " + st);
	// pS = pS.substring(ind + 1, pS.length());
	// // System.out.println(s);
	// if (pS.indexOf(":") < 1) {
	// st = pS;
	// retSrr[i + 1] = st;
	// break;
	// }
	// i++;
	// }
	// return retSrr;
	// }

	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	/**
	 * Converts a two byte array to an integer
	 * 
	 * @param b
	 *            a byte array of length 2
	 * @return an int representing the unsigned short
	 */
	// private int unsignedShortToInt(byte[] b) {
	// int i = 0;
	// i |= b[0] & 0xFF;
	// i <<= 8;
	// i |= b[1] & 0xFF;
	// return i;
	// }

	public int toInt(byte[] data) {
		if (data == null || data.length != 4)
			return 0x0;
		// ----------
		return (int) ( // NOTE: type cast not necessary for int
		(0xff & data[0]) << 24 | (0xff & data[1]) << 16 | (0xff & data[2]) << 8 | (0xff & data[3]) << 0);
	}

	// public int toLong(byte[] data) {
	// if (data == null || data.length != 8)
	// return 0x0;
	// // ----------
	// return (int) ( // NOTE: type cast not necessary for int
	// (0xff & data[0]) << 24 | (0xff & data[1]) << 16 | (0xff & data[2]) << 8 |
	// (0xff & data[3]) << 0);
	// }

	public void logJobDone(String pS) {
		this.stealTracer.append(pS);
		this.stealTracer.append("\n");
	}

	public void logJobDoneWithDate(String pS) {
		this.stealTracer.append(DateFormat.getDateTimeInstance().format(
				new Date())
				+ pS);
		this.stealTracer.append("\n");
	}

	public void logCalcTimesToFile() {
		ArrayList<JobInfo> jobs = TimeMeter.getInstance().getJobCalTimes();
		JobInfo j = null;
		Iterator<JobInfo> iter = jobs.iterator();
		this.logJobDone("Times for jobs - ");
		while (iter.hasNext()) {
			j = iter.next();
			this.logJobDone(j.deviceName + " : " + j.sendTime);
		}
	}

	public void writeJobsDoneToFile() throws IOException {
		this.writeFileWithDate(this.stealTracer.toString());
	}

	public Class<?> getClassFromName(String pName, Context pContext)
			throws NameNotFoundException, ClassNotFoundException {
		String packageName = pName.substring(0, pName.lastIndexOf("."));
		// String className = pName.substring(pName.lastIndexOf(".")+1,
		// pName.length());
		String className = pName;

		String apkName;
		apkName = pContext.getPackageManager().getApplicationInfo(packageName,
				0).sourceDir;

		PathClassLoader myClassLoader = new dalvik.system.PathClassLoader(
				apkName, ClassLoader.getSystemClassLoader());
		Class<?> handler = Class.forName(className, true, myClassLoader);
		return handler;
	}

	public final synchronized void zipDirectory(String directory,
			String zipFileName) throws IOException {
		File sdDir = Environment.getExternalStorageDirectory();
		File dir = new File(sdDir, directory);
		File f1 = new File(sdDir, zipFileName);
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f1));
		zip(dir, dir, zos);
		zos.close();
	}

	public final synchronized String zipFilesIntoDirectory(String directory,
			int startFile, int endFile, String zipFileName) throws IOException {
		File sdDir = Environment.getExternalStorageDirectory();
		File dir = new File(directory);

		// if (dir.isDirectory() && !dir.exists()) {
		// dir.mkdirs();
		// }
		StringBuffer zipName = new StringBuffer(
				getFileNameWithoutExtension(zipFileName));
		ConnectionFactory.getInstance().relock.lock();
		zipName.append(fileCount);
		fileCount++;
		ConnectionFactory.getInstance().relock.unlock();
		zipName.append(".");
		zipName.append(getFileExtension(zipFileName));

		File f2 = new File(sdDir + "/" + CommonConstants.ZIPSTORE_FILE_PATH);
		if (!f2.exists())
			f2.mkdirs();
//		String f2Name = f2.
		File f1 = new File(f2, zipName.toString());
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f1));
		zip(dir, startFile, endFile, dir, zos);
		zos.close();

		zipName.insert(0,  CommonConstants.ZIPSTORE_FILE_PATH+"/");
		return zipName.toString();
	}

	public final synchronized String zipFilesIntoDirectory(
			String[] sourceFiles, String zipFileName) throws IOException {
//		long time = System.currentTimeMillis();
		StringBuffer zipName = new StringBuffer(
				getFileNameWithoutExtension(zipFileName));
		ConnectionFactory.getInstance().relock.lock();
		zipName.append(fileCount);
		fileCount++;
		ConnectionFactory.getInstance().relock.unlock();
		zipName.append(".");
		zipName.append(getFileExtension(zipFileName));
		File sdDir = Environment.getExternalStorageDirectory();
		File[] sources = new File[sourceFiles.length];
		for (int i = 0; i < sourceFiles.length; i++) {
			if(sourceFiles[i]!=null){
				sources[i] = new File(sourceFiles[i]);//ERROR null pointer crash
			}else{
				Log.d("zipFilesIntoDirectory","sources[i] is null");
			}
			
		}
		// File dir = new File(directory);
		File f1 = new File(sdDir, zipName.toString());
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f1));
		zip(sources, zos);
		zos.close();
//		TimeMeter.getInstance().addToZipTime(
//				(System.currentTimeMillis() - time));
		
		return zipName.toString();
	}

	private final void zip(File[] sources, ZipOutputStream zos)
			throws IOException {
		// File[] files = directory.listFiles();
		byte[] buffer = new byte[CommonConstants.PACKET_SIZE];
		int read = 0;
		for (int i = 0, n = sources.length; i < n; i++) {
			// if (files[i].isDirectory()) {
			// zip(files[i], base, zos);
			// } else {
			FileInputStream in = new FileInputStream(sources[i]);
			ZipEntry entry = new ZipEntry(sources[i].getPath().substring(
					this.getDirectoryNameFromFullPath(sources[i].getPath())
							.length() + 1));
			zos.putNextEntry(entry);
			while (-1 != (read = in.read(buffer))) {
				zos.write(buffer, 0, read);
			}
			in.close();
			// }
		}
	}

	private final void zip(File directory, int startFile, int endFile,
			File base, ZipOutputStream zos) throws IOException {
		File[] files = directory.listFiles();
		byte[] buffer = new byte[CommonConstants.PACKET_SIZE];
		int read = 0;
		for (int i = startFile, n = endFile; i < n; i++) {
			if (files[i].isDirectory()) {
				zip(files[i], base, zos);
			} else {
				FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getPath().substring(
						base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}

	private final void zip(File directory, File base, ZipOutputStream zos)
			throws IOException {
		File[] files = directory.listFiles();
		byte[] buffer = new byte[CommonConstants.PACKET_SIZE];
		int read = 0;
		for (int i = 0, n = files.length; i < n; i++) {
			if (files[i].isDirectory()) {
				zip(files[i], base, zos);
			} else {
				FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getPath().substring(
						base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}

	public void unzip(File zip, File extractTo) throws IOException {
		if(zip!=null){
			ZipFile archive = new ZipFile(zip);
			Enumeration e = archive.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				File file = new File(extractTo, entry.getName());
				if (entry.isDirectory() && !file.exists()) {
					file.mkdirs();
				} else {
					if (!file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
					}

					InputStream in = archive.getInputStream(entry);
					BufferedOutputStream out = new BufferedOutputStream(
							new FileOutputStream(file));

					byte[] buffer = new byte[CommonConstants.PACKET_SIZE];
					int read;

					while (-1 != (read = in.read(buffer))) {
						out.write(buffer, 0, read);
					}

					in.close();
					out.close();
				}
			}
		}else{
			Log.d("ZIP","zip is null");
		}
		
	}

	public File[] listFilesAsArray(File directory, FilenameFilter[] filter,
			int recurse) {
		Collection<File> files = listFiles(directory, filter, recurse);

		File[] arr = new File[files.size()];
		return files.toArray(arr);
	}

	public Collection<File> listFiles(File directory, FilenameFilter[] filter,
			int recurse2) {

		Vector<File> files = new Vector<File>();

		File[] entries = directory.listFiles();

		if (entries != null) {
			for (File entry : entries) {
				for (FilenameFilter filefilter : filter) {
					if (filter == null
							|| filefilter.accept(directory, entry.getName())) {
						files.add(entry);
						Log.v("FileUtils", "Added: " + entry.getName());
					}
				}
				// if ((recurse <= -1) || (recurse > 0 && entry.isDirectory()))
				// {
				// recurse--;
				// files.addAll(listFiles(entry, filter, recurse));
				// recurse++;
				// }
			}
		}
		return files;
	}

	public String getFileFromByteArray(String pName, byte[] arr)
			throws IOException {
		File sdDir = Environment.getExternalStorageDirectory();
		StringBuffer buf = new StringBuffer(pName.substring(0,
				pName.indexOf(".")));
		ConnectionFactory.getInstance().relock.lock();
		buf.append(fileCount);
		fileCount++;
		ConnectionFactory.getInstance().relock.unlock();
		buf.append(pName.substring(pName.indexOf("."), pName.length()));

		// String name = pName.substring(0, pName.indexOf("."));
		// name +=fileCount+"";
		// String extension = pName.substring(pName.indexOf("."),
		// pName.length());
		// pName = name+extension;

		OutputStream out = null;
		try {
			out = new FileOutputStream(sdDir + "/" + buf.toString());
			out.write(arr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			out.close();
		}

		return sdDir + "/" + buf.toString();
	}

	public void resetFileCount() {
		this.fileCount = 0;
	}

	public String getFileFromByteArray2(String pName, byte[] arr)
			throws IOException {
		File sdDir = Environment.getExternalStorageDirectory();

		// String name = pName.substring(0, pName.indexOf("."));
		// name +=fileCount+"";
		// String extension = pName.substring(pName.indexOf("."),
		// pName.length());
		// pName = name+extension;

		OutputStream out = null;
		try {
			out = new FileOutputStream(sdDir + "/"
					+ CommonConstants.RECEV_FILES_PATH + "/" + pName);
			out.write(arr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			out.close();
		}

		return sdDir + "/" + CommonConstants.RECEV_FILES_PATH + "/" + pName;
	}

	public byte[] zipToBytes(String pName) throws IOException {
		ZipFile zip = new ZipFile("abc.zip");
		// now say the entry in zip file has name "abc.txt"

		ZipEntry entry = new ZipEntry("abc.txt");

		BufferedInputStream istream = new BufferedInputStream(
				zip.getInputStream(entry));
		int file_size = (int) entry.getCompressedSize();
		byte[] blob = new byte[(int) entry.getCompressedSize()];
		int bytes_read = 0;
		int offset = 0;

		while ((bytes_read = istream.read(blob, 0, file_size)) != -1) {
			offset += bytes_read;
		}

		// closing every thing
		zip.close();
		istream.close();
		return blob;
	}

	public byte[] getFileBytes(File file) throws IOException {
		ByteArrayOutputStream ous = null;
		InputStream ios = null;
		try {
			byte[] buffer = new byte[CommonConstants.PACKET_SIZE];
			ous = new ByteArrayOutputStream();
			ios = new FileInputStream(file);
			int read = 0;
			while ((read = ios.read(buffer)) != -1)
				ous.write(buffer, 0, read);
		} finally {
			try {
				if (ous != null)
					ous.close();
			} catch (IOException e) {
				// swallow, since not that important
			}
			try {
				if (ios != null)
					ios.close();
			} catch (IOException e) {
				// swallow, since not that important
			}
		}
		return ous.toByteArray();
	}

	public synchronized int getFileCount() {
		return this.fileCount;
	}

	public File getFile(String path) throws IOException {
		File sdDir = Environment.getExternalStorageDirectory();
		File file = null;
		if (sdDir.canWrite()) {
			file = new File(sdDir, path);
		}

		return file;

	}

	public File[] getFiles(ArrayList<String> list) throws IOException {
		File[] files = new File[list.size()];

		Iterator<String> iter = list.listIterator();
		int i = 0;
		while (iter.hasNext()) {
			File sdDir = Environment.getExternalStorageDirectory();
			File file = null;
			if (sdDir.canWrite()) {
				files[i] = new File(sdDir, iter.next());
			}
			i++;
		}

		return files;

	}

	public void CopyFiles(String[] files, String newPath) {
		File sdDir = Environment.getExternalStorageDirectory();
		// create a File object for the parent directory
		File newDirectory = new File(sdDir + "/" + newPath + "/");
		// have the object build the directory structure, if needed.
		newDirectory.mkdirs();
		// create a File object for the output file

		for (String filename : files) {
			try {

				File sourceFile = new File(filename);
				BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(sourceFile), 4096);
				File targetFile = new File(newDirectory,
						getFileNameFromFullPath(filename));
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(targetFile), 4096);
				int theChar;
				while ((theChar = bis.read()) != -1) {
					bos.write(theChar);
				}
				bos.close();
				bis.close();
				System.out.println("copy done!");

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	public void CopyFiles_(String[] files, String newPath) {
		for (String filename : files) {
			InputStream in = null;
			OutputStream out = null;
			File sdDir = Environment.getExternalStorageDirectory();
			try {
				// in = assetManager.open(filename);
				File f1 = new File(filename);
				File f2 = new File(sdDir + "/" + newPath + "/",
						getFileNameFromFullPath(filename));
				in = new FileInputStream(f1);
				out = new FileOutputStream(f2, false);
				// out = new FileOutputStream("/sdcard/" + filename);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch (Exception e) {
				Log.e("tag", e.getMessage());
			}
		}

	}

	public String getFileNameFromFullPath(String pName) {
		String s = pName.substring(pName.lastIndexOf("/") + 1, pName.length());
		return s;
	}

	public String getDirectoryNameFromFullPath(String pName) {
		String s = pName.substring(0, pName.lastIndexOf("/"));
		return s;
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	private String getFileNameWithoutExtension(String pName) {
		String s = pName.substring(0, pName.lastIndexOf("."));
		return s;
	}

	public String getFileName(String pName) {
		String s = pName.substring(pName.lastIndexOf("/"), pName.length());
		return s;
	}

	public String getFileExtension(String pFile) {
		return pFile.substring(pFile.lastIndexOf(".") + 1, pFile.length());
	}

	public void deleteFolderContents(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolderContents(f);
				} else {
				
					Log.d("NOTdeleteFolderContents", f.getAbsolutePath());
					f.delete();
				}
			}
		}
		// folder.delete();
	}

	public void deleteFile(String pName) {
		File f = new File(pName);
		if (f != null) {
			f.delete();
		}
	}

}
