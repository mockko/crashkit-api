package com.yoursway.crashkit.internal.utils;

import java.io.File;

public enum OS {

	WINDOWS {
		public File applicationDataFolder(String friendlyAppName) {
			String appData = System.getenv("APPDATA");
			if (appData == null)
				// don't know when this is possible, but just in case
				return new File(System.getProperty("user.home"),
						"Application Data");
			return new File(new File(appData), friendlyAppName);
		}
	},

	MAC_OS_X {
		public File applicationDataFolder(String friendlyAppName) {
			File home = new File(System.getProperty("user.home"));
			return new File(new File(new File(home, "Library"),
					"Application Support"), friendlyAppName);
		}
	},

	GENERAL_UNIX {
		public File applicationDataFolder(String friendlyAppName) {
			File home = new File(System.getProperty("user.home"));
			String dirName = "."
					+ friendlyAppName.replaceAll("\\s+", "_").toLowerCase();
			return new File(home, dirName);
		}
	};

	public static final OS current = detectOS();

	private static OS detectOS() {
		String name = System.getProperty("os.name");
		if (name.contains("Win"))
			return WINDOWS;
		if (name.contains("Mac"))
			return MAC_OS_X;
		return GENERAL_UNIX;
	}

	public abstract File applicationDataFolder(String friendlyAppName);

}
