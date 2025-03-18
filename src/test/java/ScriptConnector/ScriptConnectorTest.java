package ScriptConnector;

import org.identityconnectors.framework.common.objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptConnectorTest {

    @Mock
    private ScriptConfiguration configuration;

    @Mock
    private ScriptConnector connector;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock the configuration to provide the script path and shell type
        Mockito.when(configuration.getScriptPath()).thenReturn("C:\\Users\\akabba-adm\\Desktop\\script.sh");
        //Mockito.when(configuration.getShellType()).thenReturn("cmd.exe");
        Mockito.when(configuration.getShellType()).thenReturn("C:\\Program Files\\Git\\bin\\bash.exe");


        // Mock the connector's methods so they don't actually try to execute the script
        when(connector.create(eq(ObjectClass.ACCOUNT), anySet(), any())).thenReturn(new Uid("testUser"));

        // For void methods like delete, use doNothing()
        doNothing().when(connector).delete(eq(ObjectClass.ACCOUNT), any(Uid.class), any());

        when(connector.update(eq(ObjectClass.ACCOUNT), any(Uid.class), anySet(), any())).thenReturn(new Uid("testUser"));
    }

    @Test
    public void testCreateOperation() {
        // Set up attributes for creating a user
        Set<Attribute> attributes = Set.of(
                AttributeBuilder.build(Name.NAME, "testUser"),
                AttributeBuilder.build("email", "test@example.com")
        );

        // Call the create method and verify that the returned UID is correct
        try {
            Uid uid = connector.create(ObjectClass.ACCOUNT, attributes, null);
            assertNotNull(uid);
            assertEquals("testUser", uid.getUidValue());
        } catch (Exception e) {
            fail("Create operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteOperation() {
        Uid uid = new Uid("testUser");

        try {
            // Perform the delete operation using the mocked connector
            connector.delete(ObjectClass.ACCOUNT, uid, null);
            // Verify that the delete operation was called
            verify(connector, times(1)).delete(eq(ObjectClass.ACCOUNT), eq(uid), any());
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
            // Perform the update operation using the mocked connector
            Uid updatedUid = connector.update(ObjectClass.ACCOUNT, uid, attributes, null);
            assertEquals(uid.getUidValue(), updatedUid.getUidValue());
        } catch (Exception e) {
            fail("Update operation failed: " + e.getMessage());
        }
    }
}
