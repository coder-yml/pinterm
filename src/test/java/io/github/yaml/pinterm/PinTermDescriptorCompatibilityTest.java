package io.github.yaml.pinterm;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermDescriptorCompatibilityTest {
    @Test
    public void pluginDescriptorDependsOnTerminalAndAllowsBuild262() throws Exception {
        String pluginXml = Files.readString(Path.of("src/main/resources/META-INF/plugin.xml"));
        String gradleProperties = Files.readString(Path.of("gradle.properties"));

        assertTrue(pluginXml.contains("<id>io.github.yaml.pinterm</id>"));
        assertTrue(pluginXml.contains("require-restart=\"false\""));
        assertTrue(pluginXml.contains("<depends>org.jetbrains.plugins.terminal</depends>"));
        assertFalse(pluginXml.contains("preload="));
        assertTrue(pluginXml.contains("topic=\"com.intellij.ide.plugins.DynamicPluginListener\""));
        assertTrue(pluginXml.contains("url=\"https://github.com/coder-yml/pinterm\""));
        assertTrue(visibleDescription(pluginXml).startsWith("Pin pinned terminals"));
        assertTrue(visibleDescription(pluginXml).length() >= 40);
        assertTrue(pluginXml.contains("email=\"coder-yaml@qq.com\""));
        assertTrue(pluginXml.contains("icon=\"/icons/pinterm.svg\""));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/META-INF/pluginIcon.svg")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/icons/pinterm.svg")));
        assertTrue(gradleProperties.contains("sinceBuild = 262"));
        assertFalse(pluginXml.contains("until-build"));
        assertFalse(gradleProperties.contains("untilBuild"));
    }

    /** Marketplace rejects a description whose visible text does not start with Latin characters. */
    private static String visibleDescription(String pluginXml) {
        int start = pluginXml.indexOf("<description>");
        int end = pluginXml.indexOf("</description>");
        String body = pluginXml.substring(start, end);
        int cdata = body.indexOf("<![CDATA[");
        if (cdata >= 0) {
            body = body.substring(cdata + "<![CDATA[".length());
        }
        return body.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
