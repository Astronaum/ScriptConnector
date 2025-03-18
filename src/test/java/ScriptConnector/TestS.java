package ScriptConnector;

import org.identityconnectors.framework.common.objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestS {

    private ScriptConfiguration configuration;
    private ScriptConnector connector;

    @BeforeEach
    public void setUp() {
        configuration = new ScriptConfiguration();
        configuration.setScriptPath("C:\\midpoint\\var\\scripts\\script.ps1");
        configuration.setShellType("powershell");
        connector = new ScriptConnector();
        connector.init(configuration);
    }

    @Test
    public void testCreateOperation() {
        // Set up attributes for creating a user
        Set<Attribute> attributes = Set.of(
                AttributeBuilder.build(Name.NAME, "testUser"),
                AttributeBuilder.build("email", "test@example.com")
        );

        try {
            // Call the create method
            Uid uid = connector.create(ObjectClass.ACCOUNT, attributes, null);

            // Verify that the returned UID is correct
            assertNotNull(uid);
            assertEquals("testUser", uid.getUidValue());

            // Now run the script (powershell in this case)
            ProcessBuilder processBuilder = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-File", configuration.getScriptPath());
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Verify that the script executed successfully
            assertEquals(0, exitCode, "The script did not execute successfully");

            // Optionally, capture the output of the script
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);  // Print the output of the script
                    assertNotNull(line, "Script output should not be null");
                }
            }
        } catch (Exception e) {
            fail("Create operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteOperation() {
        Uid uid = new Uid("testUser");

        try {
            // Perform the delete operation using the connector
            connector.delete(ObjectClass.ACCOUNT, uid, null);

            // Run the script after delete operation
            ProcessBuilder processBuilder = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-File", configuration.getScriptPath());
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Verify the script executed successfully
            assertEquals(0, exitCode, "The script did not execute successfully");

            // Capture and print the script output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    assertNotNull(line, "Script output should not be null");
                }
            }

            // Optionally, verify if the user is deleted by querying the system
            // This depends on your environment and logic to confirm deletion

        } catch (Exception e) {
            fail("Delete operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateOperation() {
        Uid uid = new Uid("testUser");
        Set<Attribute> attributes = Set.of(
                AttributeBuilder.build("email", "newemail@example.com")
        );

        try {
            // Perform the update operation using the connector
            Uid updatedUid = connector.update(ObjectClass.ACCOUNT, uid, attributes, null);
            assertEquals(uid.getUidValue(), updatedUid.getUidValue());

            // Run the script after update operation
            ProcessBuilder processBuilder = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-File", configuration.getScriptPath());
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Verify the script executed successfully
            assertEquals(0, exitCode, "The script did not execute successfully");

            // Capture and print the script output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    assertNotNull(line, "Script output should not be null");
                }
            }
        } catch (Exception e) {
            fail("Update operation failed: " + e.getMessage());
        }
    }
}
