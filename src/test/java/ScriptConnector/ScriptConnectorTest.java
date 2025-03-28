package ScriptConnector;

import ScriptConnector.ScriptConfiguration;
import ScriptConnector.ScriptConnector;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.identityconnectors.framework.spi.operations.*;

import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ScriptConnectorTest {

    private ScriptConnector connector;
    private ScriptConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new ScriptConfiguration();
        configuration.setScriptPath("C:\\midpoint\\var\\scripts\\script.ps1"); // Change to your script path
        configuration.setShellType("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"); // Adjust to your shell type
        configuration.setSchemaFilePath("C:\\midpoint\\var\\schema\\schema.json"); // Change to your schema file path

        connector = new ScriptConnector();
        connector.init(configuration);
    }

    @Test
    public void testCreate() {
        ObjectClass accountClass = ObjectClass.ACCOUNT;
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build(Name.NAME, "testuser"));
        attributes.add(AttributeBuilder.build("email", "testuser@example.com"));

        try {
            Uid uid = connector.create(accountClass, attributes, null);
            assertNotNull(uid);
        } catch (Exception e) {
            fail("Create operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testUpdate() {
        ObjectClass accountClass = ObjectClass.ACCOUNT;
        Uid uid = new Uid("testuser");
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build("email", "newemail@example.com"));

        try {
            Uid updatedUid = connector.update(accountClass, uid, attributes, null);
            assertNotNull(updatedUid);
            assertEquals("testuser", updatedUid.getUidValue()); // Ensure uid is the same after update
        } catch (Exception e) {
            fail("Update operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testDelete() {
        ObjectClass accountClass = ObjectClass.ACCOUNT;
        Uid uid = new Uid("testuser");

        try {
            connector.delete(accountClass, uid, null);
            // Test passed if no exception is thrown, you can add assertions based on your script output
        } catch (Exception e) {
            fail("Delete operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testSchema() {
        try {
            // Get the schema
            Schema schema = connector.schema();
            assertNotNull(schema);

            // Retrieve the object classes
            Set<ObjectClassInfo> objectClasses = schema.getObjectClassInfo();
            assertNotNull(objectClasses);
            assertFalse(objectClasses.isEmpty()); // Ensure at least one object class is defined

            // Find the ACCOUNT object class
            ObjectClassInfo accountInfo = null;
            for (ObjectClassInfo objClass : objectClasses) {
                if (ObjectClass.ACCOUNT_NAME.equals(objClass.getType())) {
                    accountInfo = objClass;
                    break;
                }
            }

            assertNotNull(accountInfo, "ACCOUNT object class should be defined in schema");

            // Ensure that at least one attribute is defined
            assertFalse(accountInfo.getAttributeInfo().isEmpty());

            // Optionally, check if an expected attribute exists
            boolean emailFound = accountInfo.getAttributeInfo().stream()
                    .anyMatch(attr -> attr.getName().equals("email"));
            assertTrue(emailFound, "Email attribute should be present in the schema");

        } catch (Exception e) {
            fail("Schema operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testSearch() {
        ObjectClass accountClass = ObjectClass.ACCOUNT;
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build("email", "testuser@example.com"));

        try {
            ResultsHandler resultsHandler = new ResultsHandler() {
                @Override
                public boolean handle(ConnectorObject connectorObject) {
                    assertNotNull(connectorObject.getUid());
                    assertEquals("testuser", connectorObject.getUid().getUidValue());
                    return true;
                }
            };

            connector.executeQuery(accountClass, null, resultsHandler, null);
        } catch (Exception e) {
            fail("Search operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidScriptPath() {
        configuration.setScriptPath("/invalid/path/to/script.ps1");

        try {
            connector.init(configuration);
            fail("Expected IllegalArgumentException due to invalid script path.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Script path must be provided and cannot be empty"));
        }
    }
}
