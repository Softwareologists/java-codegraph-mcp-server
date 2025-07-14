import org.junit.Test;
import static org.junit.Assert.*;

public class PluginDescriptorTest {
    @Test
    public void pluginXml_present_inClassPath() {
        assertNotNull(
            PluginDescriptorTest.class.getClassLoader().getResource("META-INF/plugin.xml"));
    }
}
