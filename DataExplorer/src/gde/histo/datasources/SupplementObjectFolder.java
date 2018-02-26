/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.datasources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.DirectoryScanner.SourceDataSet;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * Folder for additional object directories in the working directory.
 * Supports caching external directory log files in this folder.
 * @author Thomas Eickert (USER)
 */
public final class SupplementObjectFolder {
	private static final String	$CLASS_NAME	= SupplementObjectFolder.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	public static Path getObjectsPath() {
		return Paths.get(Settings.getInstance().getDataFilePath(), SUPPLEMENT_DIR_NAME);
	}

	/**
	 * @return the mirrored source paths along with the timestamp of the last file copy activity
	 */
	public static Map<Path, Long> getCopyUtc() throws IOException {
		Stream<Path> stream = getObjectFolders();
		Map<Path, Long> collect = stream.collect(Collectors.toMap(p -> p, p -> p.toFile().lastModified()));
		return collect;
	}

	/**
	 * @return the mirrored source paths which fit to the current object key
	 */
	public static Stream<Path> getObjectFolders() throws IOException {
		Stream<Path> stream = DirectoryScanner.defineObjectPathsSilently(getObjectsPath(), Stream.of(DataExplorer.getInstance().getObjectKey()));
		return stream;
	}

	public static String resetFolders() {
		String message = "";
		Path objectsPath = getObjectsPath();
		int initialSize_MiB = (int) FileUtils.size(objectsPath) / 1024 / 1024;

		try {
			StringBuilder sb = new StringBuilder("  in ").append(objectsPath.toString()).append(GDE.STRING_BLANK_COLON_BLANK);
			int[] i = new int[] { 0 };
			Stream<Path> stream = DirectoryScanner.defineObjectPathsSilently(objectsPath, Settings.getInstance().getRealObjectKeys().stream());
			stream.sorted(Comparator.reverseOrder()).forEach(p -> {
				FileUtils.deleteDirectory(p.toString());
				i[0]++;
				if (i[0] < 22) {
					sb.append(GDE.STRING_NEW_LINE).append(GDE.STRING_BLANK).append(objectsPath.relativize(p));
				} else if (i[0] == 22) {
					sb.append(GDE.STRING_NEW_LINE).append(GDE.STRING_BLANK).append(p.relativize(objectsPath)).append(GDE.STRING_NEW_LINE).append(GDE.STRING_ELLIPSIS);
				}
			});

			int deletedSize_MiB = (int) FileUtils.size(objectsPath) / 1024 / 1024;
			message = Messages.getString(MessageIds.GDE_MSGT0924, new Object[] { initialSize_MiB, deletedSize_MiB, sb.toString() });
			log.log(Level.OFF, message);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			message = "File access error";
		}
		return message;
	}

	private final static String	SUPPLEMENT_DIR_NAME	= "_SupplementObjectDirs";
	private final static String	README_NAME					= "README.TXT";
	private final static String	PERMISSION_555			= "555";
	private final static String	PATH_RESOURCE				= "resource/";
	private final static int		FILE_COUNTER_MAX		= 1111;;
	private final static String	REPORT_NAME					= "report.txt";

	private final Path					supplementFolder;
	private final Set<String>		logFileExtentions		= DataExplorer.getInstance().getDeviceConfigurations().getValidLogExtentions();;

	/**
	 * Creates the base folder for additional logs which are created by other systems (computers, RC transmitters, etc).
	 * @param dataFilePath is the directory defined by the user which holds the accessible log files in device folders or object folders
	 */
	public SupplementObjectFolder(Path dataFilePath) {
		this.supplementFolder = dataFilePath.resolve(SUPPLEMENT_DIR_NAME);
		checkAndCreate();
	}

	private void checkAndCreate() {
		FileUtils.checkDirectoryAndCreate(supplementFolder.toString());
		FileUtils.extract(this.getClass(), README_NAME, PATH_RESOURCE, supplementFolder.toString(), PERMISSION_555);
	}

	/**
	 * Scan the mirror source folders asynchronously.
	 */
	public void updateLogMirror() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				Thread rebuildThread = new Thread((Runnable) () -> rebuildLogMirror(), "rebuildLogMirror");
				try {
					rebuildThread.start();
				} catch (RuntimeException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		});
	}

	private void rebuildLogMirror() {
		log.log(Level.OFF, "start rebuildLogMirror");

		Path externalBaseDir = Paths.get("E:/Logs");
		log.log(Level.FINER, "externalBaseDir=", externalBaseDir);
		rebuildLogMirror(externalBaseDir);
		log.log(Level.OFF, "end1 rebuildLogMirror");

		rebuildLogMirror(Paths.get("//Fritz-nas/fritz.nas/Generic-STORAGEDEVICE-01"));

		log.log(Level.OFF, "end  rebuildLogMirror");
	}

	private void rebuildLogMirror(Path externalBaseDir) {
		Path targetBaseDir = supplementFolder.resolve(getTargetBasePath(externalBaseDir));

		List<Path> result = new ArrayList<Path>();
		try {
			result = Files.walk(externalBaseDir).filter(Files::isDirectory) //
					.collect(Collectors.toList());
			for (Path path : result) {
				log.log(Level.OFF, "sourcePath=", path);
				Path relativeSubPath = externalBaseDir.relativize(path);
				copyDirectoryFiles(path, targetBaseDir.resolve(relativeSubPath));
			}
		} catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), " is not accessible : " + e.getClass());
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}

	}

	/**
	 * @param srcDir is the directory path holding the source files
	 * @param targetDir is the directory path dedicated for all log file types (*.osd, *.bin, ...)
	 * @throws IOException
	 */
	private void copyDirectoryFiles(Path srcDir, Path targetDir) throws IOException {
		long startNanoTime = System.nanoTime();

		int copyCounter = 0;
		final long fileCounterPriorCopy;
		final long freeSpacePriorCopy_MiB = targetDir.getRoot().toFile().getFreeSpace() / 1024 / 1024;
		if (targetDir.toFile().exists()) {
			fileCounterPriorCopy = Files.list(targetDir).limit(FILE_COUNTER_MAX).count();
		} else {
			fileCounterPriorCopy = -1;
		}

		CopyOption[] options = new CopyOption[] { //
				// StandardCopyOption.REPLACE_EXISTING, //
				StandardCopyOption.COPY_ATTRIBUTES, //
				// LinkOption.NOFOLLOW_LINKS //
		};

		try (DirectoryStream<Path> files = Files.newDirectoryStream(srcDir)) {
			for (Path f : files) {
				if (f.toFile().isDirectory()) continue;

				boolean isLogFile = logFileExtentions.parallelStream().anyMatch(s -> f.getFileName().toString().toLowerCase().endsWith(s));
				if (!isLogFile) continue;

				Path targetPath = targetDir.resolve(f.getFileName());
				FileUtils.checkDirectoryAndCreate(targetDir.toString());
				if (!targetPath.toFile().exists()) {
					SourceDataSet sourceDataSet = new DirectoryScanner.SourceDataSet(f.toFile());
					if (sourceDataSet.getDataSetType() != null) { // check if supported by histo
						File actualFile = sourceDataSet.getActualFile();
						Files.copy(actualFile.toPath(), targetPath, options);
						copyCounter++;
						log.log(Level.OFF, "copy done ", targetPath);
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		} finally {
			if (targetDir.toFile().exists()) {
				if (copyCounter > 0) {
					writeReportFile(srcDir, targetDir, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime + 500000), fileCounterPriorCopy, copyCounter, freeSpacePriorCopy_MiB);
				} else if (fileCounterPriorCopy > 0) { // this is folder with previously copied files
					boolean anyFiles = Files.list(targetDir).anyMatch(p -> !p.toFile().isDirectory());
					if (anyFiles) appendReportFile(srcDir, targetDir);
				}
			}
		}

	}

	/**
	 * @param sourceDir
	 * @param targetDir
	 * @param millis is the number of elapsed milliseconds
	 * @param fileCounterPriorCopy is the number of files in the target directory or -1 if the directory does not exist
	 * @param copyCounter is the number of files copied successfully
	 * @param freeSpacePriorCopy_MiB is the free space on the volume
	 */
	private void writeReportFile(Path sourceDir, Path targetDir, long millis, long fileCounterPriorCopy, int copyCounter, long freeSpacePriorCopy_MiB) {
		// todo check if using a property file is a better solution
		long fileCounterPostCopy; // this figure might cost a lot of elapsed time: decide if better to remove entirely
		try {
			fileCounterPostCopy = Files.list(targetDir).limit(FILE_COUNTER_MAX * 2).count();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return;
		}
		long freeSpaceAfterCopy_MiB = targetDir.toFile().getFreeSpace() / 1024 / 1024;

		String result = String.format("sourceDir=%s" + GDE.STRING_NEW_LINE //
				+ "targetDir=%s" + GDE.STRING_NEW_LINE //
				+ "numberOfFiles   : prior=%,d post=%,d copyAttempts=%,d" + GDE.STRING_NEW_LINE //
				+ "freeSpace [MiB] : prior=%,d post=%,d" + GDE.STRING_NEW_LINE //
				+ "duration [ms]   : elapsed=%,d" + GDE.STRING_NEW_LINE //
				+ "updated=%s", //
				sourceDir, targetDir, //
				fileCounterPriorCopy, fileCounterPostCopy, copyCounter, //
				freeSpacePriorCopy_MiB, freeSpaceAfterCopy_MiB, //
				millis, //
				Instant.now().toString());

		try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetDir.resolve(REPORT_NAME).toFile()), "UTF-8"))) {
			out.write(result);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Extend the report file.
	 * Used after having checked the files in the directories without any copy activity.
	 */
	private void appendReportFile(Path sourceDir, Path targetDir) {
		String result = String.format(GDE.STRING_NEW_LINE //
				+ "checked=%s", //
				Instant.now().toString());

		try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetDir.resolve(REPORT_NAME).toFile(), true), "UTF-8"))) {
			out.write(result);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @param externalPath is a path with subdirectories ('E:\Logs')
	 * @return the replacement path condensed to a single directory as a relative path ('E:_Logs')
	 */
	private Path getTargetBasePath(Path externalPath) {
		String finalPath = externalPath.toString() //
				.replaceAll(GDE.FILE_SEPARATOR_UNIX, GDE.STRING_UNDER_BAR) //
				.replaceAll(Matcher.quoteReplacement(GDE.FILE_SEPARATOR_WINDOWS), GDE.STRING_UNDER_BAR) //
				.replaceAll(GDE.STRING_COLON, GDE.STRING_UNDER_BAR);
		log.log(Level.OFF, "finalPath=", finalPath);
		return Paths.get(finalPath);
	}

}
