package org.perazimchurch.app;

import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;

public class BackupConfigurationTest {
    private static final Set<String> PRIVATE_DOMAINS = new HashSet<String>() {{
        add("root"); add("database"); add("sharedpref"); add("file"); add("external");
        add("device_root"); add("device_file"); add("device_database"); add("device_sharedpref");
    }};

    private Document readXml(String path) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
    }

    private void assertExcluded(Document doc) {
        Set<String> excluded = new HashSet<>();
        for (int i = 0; i < doc.getElementsByTagName("exclude").getLength(); i++) {
            excluded.add(doc.getElementsByTagName("exclude").item(i).getAttributes().getNamedItem("domain").getNodeValue());
        }
        assertEquals(PRIVATE_DOMAINS, excluded);
    }

    @Test public void backupIsDisabledAndAllDomainsAreExcluded() throws Exception {
        Document manifest = readXml("src/main/AndroidManifest.xml");
        assertEquals("false", manifest.getElementsByTagName("application").item(0)
                .getAttributes().getNamedItem("android:allowBackup").getNodeValue());
        assertExcluded(readXml("src/main/res/xml/backup_rules.xml"));

        Document extraction = readXml("src/main/res/xml/data_extraction_rules.xml");
        assertExcluded((Document) extraction.cloneNode(true));
        for (String section : new String[] {"cloud-backup", "device-transfer"}) {
            Set<String> sectionDomains = new HashSet<>();
            org.w3c.dom.Node parent = extraction.getElementsByTagName(section).item(0);
            for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && "exclude".equals(child.getNodeName())) {
                    sectionDomains.add(child.getAttributes().getNamedItem("domain").getNodeValue());
                }
            }
            assertEquals(PRIVATE_DOMAINS, sectionDomains);
        }
    }
}
