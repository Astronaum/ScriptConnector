package ScriptConnector;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import static org.identityconnectors.framework.common.objects.AttributeBuilder.build;

@ConnectorClass(configurationClass = ScriptConfiguration.class, displayNameKey = "script.connector.display")
public class ScriptConnector implements Connector, CreateOp, DeleteOp, UpdateOp, SchemaOp, TestOp, SearchOp<Object>{

    private static final Logger LOGGER = Logger.getLogger(ScriptConnector.class.getName());
    private ScriptConfiguration configuration;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = (ScriptConfiguration) configuration;
        LOGGER.info("ScriptConnector initialized with script path: " + this.configuration.getScriptPath());
        //LOGGER.info("ScriptConnector initialized with schema path: " + this.configuration.getSchemaFilePath());
        LOGGER.info("ScriptConnector initialized with Script Hash : " + this.configuration.getScriptHash());

        if (!verifyScriptHash()) {
            throw new SecurityException("Script hash verification failed. Script may have been modified.");
        }
    }

    @Override
    public void dispose() {
        this.configuration = null;
        LOGGER.info("ScriptConnector disposed");
    }

    @Override
    public void test() {
        // This method is called to test if the connector can interact with the service.
        try {
            // For example, you can run a simple command or script to check if the connector is operational.
            String[] command = buildCommand("test", Collections.emptySet());
            String output = executeScript(command);
            LOGGER.info("Test operation output: " + output);

            // You can add more sophisticated logic to test the connection, like checking if
            // the output is valid or if the connection was successful.
            if (output.contains("Test success")) {
                LOGGER.info("Test operation succeeded.");
            } else {
                LOGGER.warning("Test operation failed: Unexpected output.");
                throw new RuntimeException("Test operation failed: Unexpected output.");
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Test operation failed: " + e.getMessage());
            throw new RuntimeException("Test operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {

        LOGGER.info("Creating object with attributes: ");
        for (Attribute attribute : attributes) {
            LOGGER.info("Attribute name: " + attribute.getName() + ", values: " + attribute.getValue());
        }

        if (!objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new UnsupportedOperationException("Only ACCOUNT object class is supported");
        }

        try {
            String[] command = buildCommand("create", attributes);
            String output = executeScript(command);
            LOGGER.info("Create operation output: " + output);

            String uidValue = extractUidFromOutput(output, attributes);
            if (uidValue == null) {
                throw new RuntimeException("Script did not return a valid UID");
            }
            LOGGER.info("Extracted UID: " + uidValue);
            return new Uid(uidValue);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute create script: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        if (!objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new UnsupportedOperationException("Only ACCOUNT object class is supported");
        }

        try {
            // Création dynamique de l'attribut "uid"
            Set<Attribute> attributes = new HashSet<>();
            attributes.add(build("uid", uid.getUidValue()));

            String[] command = buildCommand("delete", attributes);
            LOGGER.info("Executing delete command: " + String.join(" ", command));
            String output = executeScript(command);
            LOGGER.info("Delete operation output: " + output);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute delete script: " + e.getMessage(), e);
        }
    }

    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
        if (!objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            throw new UnsupportedOperationException("Only ACCOUNT object class is supported");
        }

        try {
            String[] command = buildCommand("update", attributes,"uid=" + uid.getUidValue());
            LOGGER.info("Executing update command: " + Arrays.toString(command));
            String output = executeScript(command);
            LOGGER.info("Update operation output: " + output);
            return uid;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute update script: " + e.getMessage(), e);
        }
    }

    /*@Override
    public Schema schema() {
        LOGGER.info("Building schema from dynamic script...");
        SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());
        ObjectClassInfoBuilder accountBuilder = new ObjectClassInfoBuilder();
        accountBuilder.setType(ObjectClass.ACCOUNT_NAME);

        // Prepare command
        String[] command = new String[]{
                "powershell", "-ExecutionPolicy", "Bypass", "-File", configuration.getSchemaFilePath()
        };

        String[] command = new String[]{
                "powershell", "-ExecutionPolicy", "Bypass", "-File", configuration.getScriptPath(), "getSchema"
        };

        String output;
        try {
            output = executeScript(command);
        } catch (Exception e) {
            throw new RuntimeException("Error executing schema script: " + e.getMessage(), e);
        }

        String[] lines = output.split("\\R"); // Split by newlines (cross-platform)
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // Expecting format: name:type:requiredOrOptional
            String[] parts = line.split(":");
            if (parts.length < 3) {
                LOGGER.warning("Invalid schema line: " + line);
                continue;
            }

            String name = parts[0].trim();
            String typeStr = parts[1].trim();
            String requiredStr = parts[2].trim();

            Class<?> type = String.class;
            if ("Integer".equalsIgnoreCase(typeStr)) {
                type = Integer.class;
            } else if ("Boolean".equalsIgnoreCase(typeStr)) {
                type = Boolean.class;
            }

            boolean required = "required".equalsIgnoreCase(requiredStr);

            AttributeInfoBuilder attrBuilder = AttributeInfoBuilder.define(name).setType(type);
            if (required) {
                attrBuilder.setRequired(true);
            }

            accountBuilder.addAttributeInfo(attrBuilder.build());
        }
        schemaBuilder.defineObjectClass(accountBuilder.build());
        return schemaBuilder.build();
    }*/

    @Override
    public Schema schema() {
        LOGGER.info("Building schema from dynamic script...");
        SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());
        ObjectClassInfoBuilder accountBuilder = new ObjectClassInfoBuilder();
        accountBuilder.setType(ObjectClass.ACCOUNT_NAME);

        // Prepare command based on script language
        String[] command = prepareScriptCommand(configuration.getScriptPath(), configuration.getShellType());

        String output;
        try {
            output = executeScript(command);
        } catch (Exception e) {
            throw new RuntimeException("Error executing schema script: " + e.getMessage(), e);
        }

        // Process the output
        String[] lines = output.split("\\R"); // Split by newlines (cross-platform)
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // Expecting format: name:type:requiredOrOptional
            String[] parts = line.split(":");
            if (parts.length < 3) {
                LOGGER.warning("Invalid schema line: " + line);
                continue;
            }

            String name = parts[0].trim();
            String typeStr = parts[1].trim();
            String requiredStr = parts[2].trim();

            // Determine the type based on the schema
            Class<?> type = String.class;
            if ("Integer".equalsIgnoreCase(typeStr)) {
                type = Integer.class;
            } else if ("Boolean".equalsIgnoreCase(typeStr)) {
                type = Boolean.class;
            }

            boolean required = "required".equalsIgnoreCase(requiredStr);

            // Add attribute to the schema
            AttributeInfoBuilder attrBuilder = AttributeInfoBuilder.define(name).setType(type);
            if (required) {
                attrBuilder.setRequired(true);
            }

            accountBuilder.addAttributeInfo(attrBuilder.build());
        }

        schemaBuilder.defineObjectClass(accountBuilder.build());
        return schemaBuilder.build();
    }

    private String[] prepareScriptCommand(String scriptPath, String language) {
        switch (language.toLowerCase()) {
            case "powershell":
                return new String[] {"powershell", "-ExecutionPolicy", "Bypass", "-File", scriptPath, "getSchema"};
            case "perl":
                return new String[] {"perl", scriptPath, "getSchema"};
            case "python":
                return new String[] {"python", scriptPath, "getSchema"};
            case "bash":
                return new String[] {"/bin/bash", scriptPath, "getschema"};
            default:
                throw new IllegalArgumentException("Unsupported script language: " + language);
        }
    }


    @Override
    public FilterTranslator<Object> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return new FilterTranslator<>() {
            @Override
            public List<Object> translate(org.identityconnectors.framework.common.objects.filter.Filter filter) {
                List<Object> filters = new ArrayList<>();
                if (filter instanceof EqualsFilter eqFilter) {
                    Attribute attr = eqFilter.getAttribute();
                    if (attr.is(Name.NAME) || attr.is("email")) {
                        if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                            filters.add(attr.getName() + "=" + attr.getValue().get(0));
                        }
                    }
                }
                return filters.isEmpty() ? Collections.singletonList(null) : filters;
            }
        };
    }

    @Override
    public void executeQuery(ObjectClass objectClass, Object filter, ResultsHandler resultsHandler, OperationOptions operationOptions) {
        if (!objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            return;
        }

        try {
            List<Object> translatedFilters = createFilterTranslator(objectClass, operationOptions)
                    .translate((org.identityconnectors.framework.common.objects.filter.Filter) filter);
            String filterArg = translatedFilters.isEmpty() ? null : (String) translatedFilters.get(0);

            String[] command = filterArg == null ?
                    new String[]{configuration.getShellType(), configuration.getScriptPath(), "search"} :
                    new String[]{configuration.getShellType(), configuration.getScriptPath(), "search", filterArg};

            String output = executeScript(command);
            LOGGER.info("Search operation output: " + output);

            for (String line : output.split("\n")) {
                if (line.trim().isEmpty()) continue;

                String name = null;
                String email = null;
                for (String part : line.split(" ")) {
                    if (part.startsWith("name=")) {
                        name = part.substring(5);
                    } else if (part.startsWith("email=")) {
                        email = part.substring(6);
                    }
                }

                if (name != null) {
                    ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                            .setObjectClass(ObjectClass.ACCOUNT)
                            .setUid(name)
                            .setName(name)
                            .addAttribute("email", email != null ? email : "");
                    resultsHandler.handle(builder.build());
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute search script: " + e.getMessage(), e);
        }
    }

    private String executeScript(String[] command) throws IOException, InterruptedException {
        LOGGER.info("Executing script command: " + String.join(" ", command));

        // Ensure the appropriate shell is used for the script type
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);  // Merge stdout and stderr
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Script exited with code " + exitCode + ": " + output);
        }
        return output.toString().trim();
    }

    private String[] buildCommand(String operation, Set<Attribute> attributes, String... extraArgs) {
        List<String> command = new ArrayList<>();

        boolean isPowerShell = configuration.getScriptPath().endsWith(".ps1");
        boolean isPerl = configuration.getScriptPath().endsWith(".pl");
        boolean isPython = configuration.getScriptPath().endsWith(".py");
        boolean isBash = configuration.getShellType().endsWith(".sh");

        if (isPowerShell) {
            command.add("powershell");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
        } else if (isPerl) {
            command.add("perl");
        } else if (isPython) {
            command.add("python");
        } else {
            command.add(configuration.getShellType());
        }

        command.add(configuration.getScriptPath());
        command.add(operation);

        // Si c'est Perl (ou Bash) ET que c'est une opération DELETE, on passe les arguments en positionnels
        if ((isPerl || isBash) && "delete".equalsIgnoreCase(operation)) {
            for (Attribute attr : attributes) {
                if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                    for (Object value : attr.getValue()) {
                        command.add(String.valueOf(value));
                    }
                }
            }
        } else {
            // Sinon on utilise les préfixes classiques
            String prefix = isPython ? "--" : "-";

            for (Attribute attr : attributes) {
                if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                    String attributeName = attr.getName().equals("__NAME__") ? "name" : attr.getName();

                    for (Object value : attr.getValue()) {
                        command.add(prefix + attributeName);
                        String stringValue = String.valueOf(value);
                        if (stringValue.contains(" ")) {
                            stringValue = "\"" + stringValue + "\"";
                        }
                        command.add(stringValue);
                    }
                }
            }
        }

        // Extra args
        if (extraArgs != null) {
            for (String arg : extraArgs) {
                if (arg.contains("=")) {
                    String[] parts = arg.split("=", 2);
                    String key = parts[0];
                    String value = parts[1];

                    String prefix = isPython ? "--" : "-";
                    command.add(prefix + key);
                    command.add(value);
                } else {
                    command.add(arg);
                }
            }
        }

        return command.toArray(new String[0]);
    }

    private String extractUidFromOutput(String output, Set<Attribute> attributes) {
        LOGGER.info("Script output received in extractUIDFromOutput: " + output);

        // Use regex to extract UID
        Pattern pattern = Pattern.compile("UID=([a-zA-Z0-9\\-]+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            String extractedUid = matcher.group(1);
            LOGGER.info("Extracted UID from script output: " + extractedUid);
            return extractedUid;
        }

        // If UID not found in output, fallback to __NAME__
        for (Attribute attr : attributes) {
            LOGGER.info("Checking attribute: " + attr.getName());
            if (attr.is(Name.NAME) && attr.getValue() != null && !attr.getValue().isEmpty()) {
                String fallbackUid = attr.getValue().get(0).toString();
                LOGGER.info("Fallback UID from Name attribute: " + fallbackUid);
                return fallbackUid;
            }
        }

        LOGGER.warning("No UID found in output or attributes.");
        return null;
    }

    /*private boolean verifyScriptHash() {
        String scriptPath = configuration.getScriptPath();
        String expectedHash = configuration.getScriptHash(); // SHA-256 attendu
        try {
            byte[] scriptBytes = Files.readAllBytes(Paths.get(scriptPath));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(scriptBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String actualHash = sb.toString();

            LOGGER.info("Expected Script Hash: " + expectedHash);
            LOGGER.info("Actual Script Hash:   " + actualHash);

            return actualHash.equalsIgnoreCase(expectedHash);
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.severe("Error computing script hash: " + e.getMessage());
            return false;
        }
    }*/

    private boolean verifyScriptHash() {
        String scriptPath = configuration.getScriptPath();
        String expectedHash = configuration.getScriptHash(); // SHA-256 attendu

        // Check if the hash is provided
        if (expectedHash == null || expectedHash.isEmpty()) {
            LOGGER.warning("No script hash provided. Executing script without verification.");
            return true;  // Skip hash verification and allow execution
        }

        try {
            // Read the script file
            byte[] scriptBytes = Files.readAllBytes(Paths.get(scriptPath));

            // Compute the script hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(scriptBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String actualHash = sb.toString();

            // Log both hashes
            LOGGER.info("Expected Script Hash: " + expectedHash);
            LOGGER.info("Actual Script Hash:   " + actualHash);

            // Verify the hashes
            if (actualHash.equalsIgnoreCase(expectedHash)) {
                return true;  // Hash matches, proceed with execution
            } else {
                LOGGER.warning("Hash mismatch! Expected hash does not match actual hash.");
                return false;  // Hash mismatch, return false (or handle it as needed)
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.severe("Error computing script hash: " + e.getMessage());
            return false;  // Return false if there's an error
        }
    }

}
