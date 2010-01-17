package com.yoursway.crashkit.internal.utils;

import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class YsFileUtils {

	public static final String UTF8_ENCODING = "utf-8";

	public static Object readAsObject(File source) throws IOException,
			ClassNotFoundException {
		FileInputStream in = new FileInputStream(source);
		try {
			ObjectInputStream oi = new ObjectInputStream(in);
			return oi.readObject();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void writeObject(Object object, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		try {
			writeObject(object, out);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void writeObject(Object object, OutputStream out)
			throws IOException {
		ObjectOutputStream oo = new ObjectOutputStream(out);
		oo.writeObject(object);
		oo.flush();
	}

	public static String readAsString(File source) throws IOException {
		return readAsString(source, UTF8_ENCODING);
	}

	public static String readAsString(File source, String encoding)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		loadFromFile(source, baos);
		byte[] bytes = baos.toByteArray();
		return new String(bytes, encoding);
	}

	public static String readAsStringAndClose(InputStream source)
			throws IOException {
		try {
			return readAsString(source, UTF8_ENCODING);
		} finally {
			source.close();
		}
	}

	public static String readAsString(InputStream source) throws IOException {
		return readAsString(source, UTF8_ENCODING);
	}

	public static String readAsString(InputStream source, String encoding)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		transfer(source, baos);
		byte[] bytes = baos.toByteArray();
		return new String(bytes, encoding);
	}

	public static void writeString(File destination, String data)
			throws IOException {
		writeString(destination, data, UTF8_ENCODING);
	}

	public static void writeString(File destination, String data,
			String encoding) throws IOException {
		writeBytes(destination, data.getBytes(encoding));
	}

	public static void writeBytes(File destination, byte[] data)
			throws IOException {
		saveToFile(new ByteArrayInputStream(data), destination);
	}

	public static void writeStringAndClose(OutputStream destination, String data)
			throws IOException {
		try {
			writeString(destination, data, UTF8_ENCODING);
		} finally {
			destination.close();
		}
	}

	public static void writeString(OutputStream destination, String data)
			throws IOException {
		writeString(destination, data, UTF8_ENCODING);
	}

	public static void writeString(OutputStream destination, String data,
			String encoding) throws IOException {
		writeBytes(destination, data.getBytes(encoding));
	}

	public static void writeBytes(OutputStream destination, byte[] data)
			throws IOException {
		transfer(new ByteArrayInputStream(data), destination);
	}

	public static void cp_r(File source, File destinationParentFolder)
			throws IOException {
		List<File> e = emptyList();
		cp_r_exclude(source, destinationParentFolder, e);
	}

	public static void cp_r_exclude(File source, File destinationParentFolder,
			Collection<File> excluded) throws IOException {
		destinationParentFolder.mkdirs();
		cp_r_(source, destinationParentFolder, excluded);
	}

	/**
	 * Prereq: <code>destinationParentFolder</code> exists.
	 * 
	 * @param excluded
	 */
	private static void cp_r_(File source, File destinationParentFolder,
			Collection<File> excluded) throws IOException {
		if (excluded.contains(source))
			return;
		if (!source.isDirectory()) {
			fileCopy(source,
					new File(destinationParentFolder, source.getName()));
		} else {
			File childrenDestination = new File(destinationParentFolder, source
					.getName());
			cp_r_children(source, childrenDestination, excluded);
		}
	}

	public static void cp_r_children(File source, File childrenDestination)
			throws IOException {
		List<File> e = emptyList();
		cp_r_children(source, childrenDestination, e);
	}

	public static void cp_r_children(File source, File childrenDestination,
			Collection<File> excluded) throws IOException {
		childrenDestination.mkdirs();

		File[] children = source.listFiles();
		if (children != null)
			for (File child : children)
				cp_r_(child, childrenDestination, excluded);
	}

	public static void fileCopy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		try {
			saveToFile(in, dst);
		} finally {
			in.close();
		}
	}

	public static void download(URL url, File dst) throws IOException {
		InputStream in = url.openStream();
		try {
			saveToFile(in, dst);
		} finally {
			in.close();
		}
	}

	public static void saveToFile(InputStream in, File dst)
			throws FileNotFoundException, IOException {
		OutputStream out = new FileOutputStream(dst);
		try {
			transfer(in, out);
		} finally {
			out.close();
		}
	}

	public static void loadFromFile(File src, OutputStream out)
			throws FileNotFoundException, IOException {
		InputStream in = new FileInputStream(src);
		try {
			transfer(in, out);
		} finally {
			in.close();
		}
	}

	public static void transfer(InputStream in, OutputStream out)
			throws IOException {
		byte[] buf = new byte[1024 * 1024];
		int len;
		while ((len = in.read(buf)) > 0)
			out.write(buf, 0, len);
	}

	public static void transfer(InputStream in, OutputStream out, int maxBytes)
			throws IOException {
		byte[] buf = new byte[maxBytes];
		int done = 0;
		int len;
		while (done < maxBytes && (len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
			done += len;
		}
	}

	public static File createTempFolder(String prefix, String suffix)
			throws IOException {
		File file;
		do {
			file = File.createTempFile(prefix, suffix);
			file.delete();
		} while (!file.mkdir());
		return file;
	}

	public static File urlToFileWithProtocolCheck(URL url) {
		if (!url.getProtocol().equals("file"))
			throw new IllegalArgumentException("URL is not a file: " + url);
		return new File(url.getPath());
	}

	public static void deleteFile(File file) {
		if (file.exists() && !file.delete())
			throw new RuntimeException("Cannot delete file " + file);
	}

	public static void deleteRecursively(File directory) {
		File[] children = directory.listFiles();
		if (children != null) {
			for (File child : children)
				if (child.isDirectory())
					deleteRecursively(child);
				else
					deleteFile(child);

			if (!directory.delete())
				throw new RuntimeException("Cannot delete directory "
						+ directory);
		}
	}

	public static void zipChildren(File folder, String prefix,
			ZipOutputStream out) throws IOException {
		File[] files = folder.listFiles();
		if (files == null)
			return;
		for (File file : files) {
			if (file.isFile()) {
				String name = prefix + file.getName();
				ZipEntry entry = new ZipEntry(name);
				entry.setTime(file.lastModified());
				out.putNextEntry(entry);
				loadFromFile(file, out);
				out.closeEntry();
			} else if (file.isDirectory()) {
				zipChildren(file, prefix + file.getName() + "/", out);
			}
		}

	}

	public static boolean isBogusFile(String name) {
		return Pattern.compile("^([._](git|svn|darcs)|CVS|\\.DS_Store)$")
				.matcher(name).find();
	}

	public static boolean isSameFile(File first, File second) {
		try {
			first = first.getCanonicalFile();
		} catch (IOException e) {
			first = first.getAbsoluteFile();
		}
		try {
			second = second.getCanonicalFile();
		} catch (IOException e) {
			second = second.getAbsoluteFile();
		}
		return first.equals(second);
	}

}
