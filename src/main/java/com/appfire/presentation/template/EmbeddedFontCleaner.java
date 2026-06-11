package com.appfire.presentation.template;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmbeddedFontCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedFontCleaner.class);
    private static final String FONT_REL_TYPE =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/font";
    private static final Pattern EMBEDDED_FONT_BLOCK = Pattern.compile(
            "<p:embeddedFont>\\s*<p:font typeface=\"([^\"]+)\"/>"
                    + "(?:\\s*<p:(?:regular|bold|italic|boldItalic) r:id=\"([^\"]+)\"/>)?"
                    + "\\s*</p:embeddedFont>");
    private static final Pattern RELATIONSHIP = Pattern.compile("<Relationship\\s+([^>]*)/>");

    private final boolean enabled;

    public EmbeddedFontCleaner(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void clean(Path pptxPath) {
        if (!enabled) {
            return;
        }
        FontCleanupPlan plan = null;
        try (OPCPackage pkg = OPCPackage.open(new File(pptxPath.toString()), PackageAccess.READ_WRITE)) {
            plan = buildPlan(pkg, pptxPath);
            if (plan == null) {
                return;
            }
            applyOpcChanges(pkg, plan);
        } catch (Exception e) {
            LOG.warn(
                    "Embedded font OPC cleanup failed. Resolution: verify template.pptx and re-run ./gradlew run",
                    e);
        }
        if (plan != null) {
            applyZipUpdates(pptxPath, plan);
        }
    }

    private FontCleanupPlan buildPlan(OPCPackage pkg, Path pptxPath) throws Exception {
        Set<String> referenced = ReferencedFontCollector.collect(pkg);
        PackagePart presentationPart = requirePart(pkg, "/ppt/presentation.xml");
        PackagePart relsPart = requirePart(pkg, "/ppt/_rels/presentation.xml.rels");

        String presentationXml = readPart(presentationPart);
        String relsXml = readPart(relsPart);
        String contentTypesXml = readZipEntry(pptxPath, "[Content_Types].xml");
        Map<String, String> relationshipTargets = mapFontRelationships(relsXml);

        List<RemovalTarget> removals = findUnusedEmbeddedFonts(presentationXml, referenced, relationshipTargets);
        LOG.info("Embedded font cleanup: {} referenced, {} unused with fntdata", referenced.size(), removals.size());
        if (removals.isEmpty()) {
            return null;
        }

        String updatedPresentationXml = presentationXml;
        String updatedRelsXml = relsXml;
        String updatedContentTypesXml = contentTypesXml;
        for (RemovalTarget removal : removals) {
            updatedPresentationXml = removeEmbeddedFontBlock(updatedPresentationXml, removal.typeface());
            updatedRelsXml = removeRelationship(updatedRelsXml, removal.relationshipId());
            updatedContentTypesXml = removeContentTypeOverride(updatedContentTypesXml, removal.targetPath());
        }

        updatedPresentationXml = removeEmptyEmbeddedFontList(updatedPresentationXml);
        return new FontCleanupPlan(
                presentationPart,
                updatedPresentationXml,
                updatedRelsXml,
                updatedContentTypesXml,
                removals);
    }

    private void applyOpcChanges(OPCPackage pkg, FontCleanupPlan plan) throws Exception {
        writePart(pkg, plan.presentationPart(), plan.updatedPresentationXml());
        for (RemovalTarget removal : plan.removals()) {
            long bytesRemoved = deleteFontPart(pkg, removal.targetPath());
            LOG.info("Removed unused embedded font '{}' ({} bytes)", removal.typeface(), bytesRemoved);
        }
    }

    private void applyZipUpdates(Path pptxPath, FontCleanupPlan plan) {
        try {
            PptxZipEntryReplacer.replaceEntry(
                    pptxPath,
                    "ppt/_rels/presentation.xml.rels",
                    plan.updatedRelsXml().getBytes(StandardCharsets.UTF_8));
            PptxZipEntryReplacer.replaceEntry(
                    pptxPath,
                    "[Content_Types].xml",
                    plan.updatedContentTypesXml().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.warn(
                    "Failed to update package metadata after embedded font cleanup. "
                            + "Resolution: set FONT_CLEANUP_ENABLED=false and re-run ./gradlew run",
                    e);
        }
    }

    private List<RemovalTarget> findUnusedEmbeddedFonts(
            String presentationXml,
            Set<String> referenced,
            Map<String, String> relationshipTargets) {
        List<RemovalTarget> removals = new ArrayList<>();
        Matcher matcher = EMBEDDED_FONT_BLOCK.matcher(presentationXml);
        while (matcher.find()) {
            String typeface = matcher.group(1);
            String relationshipId = matcher.group(2);
            if (isReferenced(typeface, referenced) || relationshipId == null) {
                continue;
            }
            String target = relationshipTargets.get(relationshipId);
            if (target == null || !target.contains(".fntdata")) {
                continue;
            }
            removals.add(new RemovalTarget(typeface, relationshipId, target));
        }
        return removals;
    }

    private long deleteFontPart(OPCPackage pkg, String targetPath) throws Exception {
        PackagePartName partName = PackagingURIHelper.createPartName("/ppt/" + normalizeTarget(targetPath));
        PackagePart part = pkg.getPart(partName);
        long size = part.getSize();
        pkg.removePart(partName);
        return size;
    }

    private String removeEmbeddedFontBlock(String presentationXml, String typeface) {
        Pattern block = Pattern.compile(
                "<p:embeddedFont>\\s*<p:font typeface=\""
                        + Pattern.quote(typeface)
                        + "\"/>\\s*<p:(?:regular|bold|italic|boldItalic) r:id=\"[^\"]+\"/>\\s*</p:embeddedFont>");
        return block.matcher(presentationXml).replaceAll("");
    }

    private String removeRelationship(String relsXml, String relationshipId) {
        Pattern relationship = Pattern.compile(
                "<Relationship\\s+[^>]*\\bId=\""
                        + Pattern.quote(relationshipId)
                        + "\"[^>]*/>");
        return relationship.matcher(relsXml).replaceAll("");
    }

    private String removeEmptyEmbeddedFontList(String presentationXml) {
        return presentationXml.replace("<p:embeddedFontLst></p:embeddedFontLst>", "");
    }

    private String removeContentTypeOverride(String contentTypesXml, String targetPath) {
        String partName = "/ppt/" + normalizeTarget(targetPath);
        Pattern override = Pattern.compile(
                "<Override PartName=\""
                        + Pattern.quote(partName)
                        + "\" ContentType=\"[^\"]+\"/>");
        return override.matcher(contentTypesXml).replaceAll("");
    }

    private Map<String, String> mapFontRelationships(String relsXml) {
        Map<String, String> targets = new java.util.HashMap<>();
        Matcher matcher = RELATIONSHIP.matcher(relsXml);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            if (!FONT_REL_TYPE.equals(readAttribute(attributes, "Type"))) {
                continue;
            }
            String id = readAttribute(attributes, "Id");
            String target = readAttribute(attributes, "Target");
            if (id != null && target != null) {
                targets.put(id, target);
            }
        }
        return targets;
    }

    private String readAttribute(String attributes, String name) {
        Matcher matcher = Pattern.compile(name + "=\"([^\"]+)\"").matcher(attributes);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isReferenced(String typeface, Set<String> referenced) {
        for (String candidate : referenced) {
            if (candidate.equalsIgnoreCase(typeface)) {
                return true;
            }
        }
        return false;
    }

    private PackagePart requirePart(OPCPackage pkg, String partUri) throws Exception {
        PackagePartName partName = PackagingURIHelper.createPartName(partUri);
        return pkg.getPart(partName);
    }

    private String readPart(PackagePart part) throws IOException {
        try (InputStream input = part.getInputStream()) {
            return new String(IOUtils.toByteArray(input), StandardCharsets.UTF_8);
        }
    }

    private String readZipEntry(Path pptxPath, String entryName) throws IOException {
        try (ZipFile zipFile = new ZipFile(pptxPath.toFile());
                InputStream input = zipFile.getInputStream(zipFile.getEntry(entryName))) {
            if (input == null) {
                throw new IOException("Zip entry not found: " + entryName);
            }
            return new String(IOUtils.toByteArray(input), StandardCharsets.UTF_8);
        }
    }

    private void writePart(OPCPackage pkg, PackagePart part, String xml) throws IOException {
        PackagePartName partName = part.getPartName();
        String contentType = part.getContentType();
        pkg.removePart(partName);
        PackagePart replacement = pkg.createPart(partName, contentType);
        try (OutputStream output = replacement.getOutputStream()) {
            output.write(xml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String normalizeTarget(String target) {
        return target.startsWith("/") ? target.substring(1) : target;
    }

    private record RemovalTarget(String typeface, String relationshipId, String targetPath) {
    }

    private record FontCleanupPlan(
            PackagePart presentationPart,
            String updatedPresentationXml,
            String updatedRelsXml,
            String updatedContentTypesXml,
            List<RemovalTarget> removals) {
    }
}
