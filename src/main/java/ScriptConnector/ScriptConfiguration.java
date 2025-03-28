package ScriptConnector;

import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import java.io.File;

public class ScriptConfiguration extends AbstractConfiguration {

    private String scriptPath;
    private String shellType;
    private String schemaFilePath;

    @ConfigurationProperty(
            order = 1,
            displayMessageKey = "Script Path",
            helpMessageKey = "Full path to the script to execute (e.g., /path/to/script.ps1)"
    )
    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    @ConfigurationProperty(
            order = 2,
            displayMessageKey = "Shell Type",
            helpMessageKey = "Shell to execute the script (e.g., '/bin/bash' for Bash, 'cmd.exe', 'PowerShell')"
    )
    public String getShellType() {
        return shellType != null ? shellType : "/bin/bash"; // Default to /bin/bash
    }

    public void setShellType(String shellType) {
        this.shellType = shellType;
    }

    @ConfigurationProperty(
            order = 3,
            displayMessageKey = "Schema Path",
            helpMessageKey = "Full path to the json schema"
    )
    public String getSchemaFilePath() {
        return schemaFilePath;
    }

    public void setSchemaFilePath(String schemaFilePath) {
        this.schemaFilePath = schemaFilePath;
    }

    @Override
    public void validate() {
        // Check scriptPath
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Script path must be provided and cannot be empty.");
        }
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists() || !scriptFile.isFile() || !scriptFile.canRead()) {
            throw new IllegalArgumentException("Script path '" + scriptPath + "' does not exist, is not a file, or is not readable.");
        }

        // Validate schemaFilePath if provided
        if (schemaFilePath == null || schemaFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema path must be provided and cannot be empty.");
        }
        File schemaFile = new File(schemaFilePath);
        if (!schemaFile.exists() || !schemaFile.isFile() || !schemaFile.canRead()) {
                throw new IllegalArgumentException("Schema file '" + schemaFilePath + "' does not exist, is not a file, or is not readable.");
        }

        // Check shellType
        if (shellType == null || shellType.trim().isEmpty()) {
            throw new IllegalArgumentException("Shell type must be provided (e.g., '/bin/bash', 'cmd.exe', 'PowerShell').");
        }
    }
}