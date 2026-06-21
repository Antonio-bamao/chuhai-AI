import ch.r;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ResourceDecoder {
    private static final List<String> DEFAULT_RESOURCES = Arrays.asList(
        "master.html",
        "msg.html",
        "fm.js",
        "country_ips.json",
        "html/Login.html",
        "html/product-selector.html",
        "html/ClawWorkspace.html"
    );

    private ResourceDecoder() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                "usage: ResourceDecoder <App.jar> <output-dir> [resource ...]"
            );
        }

        Path jarPath = Paths.get(args[0]).toAbsolutePath().normalize();
        Path outputRoot = Paths.get(args[1]).toAbsolutePath().normalize();
        List<String> resources = args.length == 2
            ? DEFAULT_RESOURCES
            : Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
        List<DecodedResource> decodedResources = new ArrayList<DecodedResource>();

        Files.createDirectories(outputRoot);
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (String resource : resources) {
                String safeResource = validateResourcePath(resource);
                JarEntry entry = jar.getJarEntry(safeResource);
                if (entry == null || entry.isDirectory()) {
                    throw new IOException("resource not found: " + safeResource);
                }

                byte[] decoded;
                try (
                    InputStream raw = jar.getInputStream(entry);
                    InputStream decrypted = new r(raw)
                ) {
                    decoded = readAll(decrypted);
                }

                Path output = outputRoot.resolve(safeResource).normalize();
                if (!output.startsWith(outputRoot)) {
                    throw new IllegalArgumentException("unsafe resource path: " + resource);
                }
                Files.createDirectories(output.getParent());
                Files.write(output, decoded);
                decodedResources.add(
                    new DecodedResource(safeResource, decoded.length, sha256(decoded))
                );
                System.out.println(safeResource + "\t" + decoded.length);
            }
        }

        Files.write(
            outputRoot.resolve("manifest.json"),
            renderManifest(jarPath, decodedResources).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String validateResourcePath(String resource) {
        if (
            resource == null
                || resource.isEmpty()
                || resource.startsWith("/")
                || resource.contains("\\")
        ) {
            throw new IllegalArgumentException("unsafe resource path: " + resource);
        }
        for (String part : resource.split("/")) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("unsafe resource path: " + resource);
            }
        }
        return resource;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String sha256(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static String renderManifest(Path jarPath, List<DecodedResource> resources) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"sourceJar\": \"").append(escapeJson(jarPath.toString())).append("\",\n");
        json.append("  \"resources\": [\n");
        for (int index = 0; index < resources.size(); index++) {
            DecodedResource resource = resources.get(index);
            json.append("    {\"resource\": \"")
                .append(escapeJson(resource.name))
                .append("\", \"bytes\": ")
                .append(resource.bytes)
                .append(", \"sha256\": \"")
                .append(resource.sha256)
                .append("\"}");
            if (index + 1 < resources.size()) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class DecodedResource {
        private final String name;
        private final int bytes;
        private final String sha256;

        private DecodedResource(String name, int bytes, String sha256) {
            this.name = name;
            this.bytes = bytes;
            this.sha256 = sha256;
        }
    }
}
