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
import java.util.*;
import java.util.logging.Logger;

@ConnectorClass(configurationClass = ScriptConfiguration.class, displayNameKey = "script.connector.display")
public class ScriptConnector implements Connector, CreateOp, DeleteOp, UpdateOp, SchemaOp, SearchOp<Object> {

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
    }

    @Override
    public void dispose() {
        this.configuration = null;
        LOGGER.info("ScriptConnector disposed");
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
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
            String[] command = {configuration.getShellType(), configuration.getScriptPath(), "delete", "uid=" + uid.getUidValue()};
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
            String[] command = buildCommand("update", attributes, "uid=" + uid.getUidValue());
            String output = executeScript(command);
            LOGGER.info("Update operation output: " + output);
            return uid;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute update script: " + e.getMessage(), e);
        }
    }

    @Override
    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(ScriptConnector.class);

        ObjectClassInfoBuilder accountBuilder = new ObjectClassInfoBuilder();
        accountBuilder.setType(ObjectClass.ACCOUNT_NAME);
        accountBuilder.addAttributeInfo(Name.INFO);
        accountBuilder.addAttributeInfo(AttributeInfoBuilder.define("email")
                .setType(String.class)
                .setRequired(false)
                .build());

        schemaBuilder.defineObjectClass(accountBuilder.build());
        return schemaBuilder.build();
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
        command.add(configuration.getShellType());
        command.add(configuration.getScriptPath());
        command.add(operation);

        for (Attribute attr : attributes) {
            if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                command.add(attr.getName() + "=" + attr.getValue().get(0));
            }
        }
        command.addAll(Arrays.asList(extraArgs));
        return command.toArray(new String[0]);
    }

    private String extractUidFromOutput(String output, Set<Attribute> attributes) {
        for (String line : output.split("\n")) {
            if (line.startsWith("UID=")) {
                return line.substring(4);
            }
        }

        for (Attribute attr : attributes) {
            if (attr.is(Name.NAME) && attr.getValue() != null && !attr.getValue().isEmpty()) {
                return attr.getValue().get(0).toString();
            }
        }
        return null;
    }
}
