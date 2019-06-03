package pcl.bridgebot;

import pcl.bridgebot.util.Version;

public class BuildInfo {
    public static final Version VERSION = new Version(
            "@versionMajor@",
            "@versionMinor@",
            "@versionRevision@",
            "@versionBuild@"
    		);
}
