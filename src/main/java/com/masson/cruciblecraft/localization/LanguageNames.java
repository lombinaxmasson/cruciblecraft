package com.masson.cruciblecraft.localization;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.material.MaterialZhNames;

/**
 * Canonical registry-backed translation keys and player-facing display names.
 *
 * <p>Shared by datagen, the generated material pack, and material-form fallbacks.
 * Registry ids, model paths, and save ids stay unchanged; only language keys and
 * display strings go through this contract.
 */
public final class LanguageNames {
    public static final String RESOURCE =
            "data/cruciblecraft/language_display_contract.json";

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern LATIN = Pattern.compile("[A-Za-z]");
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern META_SUFFIX =
            Pattern.compile("\\s+m\\d+$", Pattern.CASE_INSENSITIVE);

    private static volatile Map<String, String> prefixExceptions = Map.of();
    private static volatile Map<String, String> tokenExceptions = Map.of();
    private static volatile Map<String, String> slabFaces = Map.of();
    private static volatile Map<String, String> zhPathTokens = Map.of();
    private static volatile Set<String> dyeColors = Set.of();
    private static volatile boolean loaded;

    private LanguageNames() {}

    public static String translationKey(String objectType, String registryPath) {
        if (registryPath == null || registryPath.isEmpty()) {
            throw new IllegalArgumentException("registry_path is empty");
        }
        return objectType
                + "."
                + CrucibleCraft.MODID
                + "."
                + registryPath.replace('/', '.');
    }

    public static String formatEnglishId(String value) {
        ensureLoaded();
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("empty id");
        }
        String prefix = prefixExceptions.get(value);
        if (prefix != null) {
            return prefix;
        }
        String[] parts = value.replace('-', '_').split("_");
        List<String> rendered = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            String mapped = tokenExceptions.get(part.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                rendered.add(mapped);
                continue;
            }
            rendered.add(part.substring(0, 1).toUpperCase(Locale.ROOT)
                    + part.substring(1).toLowerCase(Locale.ROOT));
        }
        return String.join(" ", rendered);
    }

    public static String composeEnglish(String materialId, String formId) {
        return formatEnglishId(materialId) + " " + formatEnglishId(formId);
    }

    public static boolean hasCjk(String text) {
        return text != null && CJK.matcher(text).find();
    }

    public static boolean isEnglishCopy(String zh, String en) {
        if (zh == null || zh.isBlank()) {
            return false;
        }
        String value = zh.strip();
        if (hasCjk(value)) {
            return false;
        }
        if (value.contains("%s")) {
            return false;
        }
        if (en != null && value.equals(en.strip())) {
            return true;
        }
        return LATIN.matcher(value).find();
    }

    public static boolean isEnglishCopy(String zh) {
        return isEnglishCopy(zh, null);
    }

    public static Optional<String> chineseOrEmpty(String zh, String en) {
        if (zh == null || zh.isBlank() || isEnglishCopy(zh, en)) {
            return Optional.empty();
        }
        return Optional.of(zh);
    }

    public static String playerEnglish(String sourceName, String registryPath) {
        ensureLoaded();
        String path = registryPath == null ? "" : registryPath;
        String name = sourceName == null ? "" : META_SUFFIX.matcher(sourceName).replaceFirst("").strip();
        if (name.isEmpty()) {
            if (path.isEmpty()) {
                return "";
            }
            name = formatEnglishId(path.replace('/', '_'));
        }
        Set<String> owned = wordTokens(name);
        List<String> extras = new ArrayList<>();
        boolean first = true;
        for (String segment : path.replace('.', '/').split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            String label = segmentLabel(segment);
            Set<String> unused = wordTokens(label);
            unused.removeAll(owned);
            Set<String> keep = semanticTokens(unused);
            for (String token : unused) {
                if (isDigits(token) && token.length() <= 2) {
                    keep.add(token);
                }
            }
            if (first) {
                keep.removeIf(token -> !isDiscriminator(token));
            }
            first = false;
            if (keep.isEmpty() || keep.size() > 3) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            for (String word : label.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                Set<String> wordTok = wordTokens(word);
                boolean wanted = false;
                for (String token : wordTok) {
                    if (keep.contains(token)) {
                        wanted = true;
                        break;
                    }
                }
                if (wanted) {
                    parts.add(word);
                }
            }
            if (parts.isEmpty()) {
                continue;
            }
            extras.add(String.join(" ", parts));
            owned.addAll(keep);
        }
        if (extras.isEmpty()) {
            return name;
        }
        return name + " " + String.join(" ", extras);
    }

    public static Optional<String> playerChinese(String sourceName, String registryPath) {
        ensureLoaded();
        if (sourceName == null || !hasCjk(sourceName)) {
            return Optional.empty();
        }
        String name = META_SUFFIX.matcher(sourceName).replaceFirst("").strip();
        List<String> extras = new ArrayList<>();
        String path = registryPath == null ? "" : registryPath;
        for (String segment : path.replace('.', '/').split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            String mapped = zhPathTokens.get(segment);
            if (mapped == null) {
                if (isAlphabeticVariant(segment)) {
                    return Optional.empty();
                }
                continue;
            }
            if (!mapped.isEmpty() && !name.contains(mapped) && !extras.contains(mapped)) {
                extras.add(mapped);
            }
        }
        return Optional.of(name + String.join("", extras));
    }

    public static Optional<String> composeMaterialFormZh(String registryPath) {
        if (registryPath == null) {
            return Optional.empty();
        }
        int slash = registryPath.indexOf('/');
        if (slash <= 0 || registryPath.indexOf('/', slash + 1) >= 0) {
            return Optional.empty();
        }
        String material = registryPath.substring(0, slash);
        String form = registryPath.substring(slash + 1);
        return MaterialZhNames.material(material).flatMap(mat ->
                MaterialZhNames.prefix(form)
                        .or(() -> MaterialZhNames.pipe(form))
                        .or(() -> MaterialZhNames.conductor(form))
                        .map(name -> mat + name));
    }

    public static String chineseFormTemplate(String formZh) {
        return "%s" + formZh;
    }

    public static String englishFormTemplate(String formEnglish) {
        return "%s " + formEnglish;
    }

    private static String segmentLabel(String segment) {
        String face = slabFaces.get(segment);
        if (face != null) {
            return face;
        }
        return formatEnglishId(segment);
    }

    private static Set<String> wordTokens(String text) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = WORD.matcher(text == null ? "" : text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static Set<String> semanticTokens(Set<String> tokens) {
        Set<String> semantic = new HashSet<>();
        for (String token : tokens) {
            if (!isDigits(token)) {
                semantic.add(token);
            }
        }
        return semantic;
    }

    private static boolean isDigits(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDiscriminator(String token) {
        ensureLoaded();
        if (tokenExceptions.containsKey(token) || dyeColors.contains(token)) {
            return true;
        }
        if (slabFaces.containsKey(token) || zhPathTokens.containsKey(token)) {
            return true;
        }
        if (token.length() == 1 && Character.isLetter(token.charAt(0))) {
            return true;
        }
        return isDigits(token) && token.length() <= 2;
    }

    private static boolean isAlphabeticVariant(String segment) {
        boolean letters = false;
        boolean digits = false;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (Character.isLetter(c)) {
                letters = true;
            } else if (Character.isDigit(c)) {
                digits = true;
            }
        }
        return letters && !digits;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (LanguageNames.class) {
            if (loaded) {
                return;
            }
            try (InputStream input = LanguageNames.class.getClassLoader()
                    .getResourceAsStream(RESOURCE)) {
                if (input == null) {
                    throw new IllegalStateException("Missing " + RESOURCE);
                }
                JsonObject root = JsonParser.parseReader(
                                new InputStreamReader(input, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                prefixExceptions = mapOf(root.getAsJsonObject("prefix_exceptions"));
                tokenExceptions = mapOf(root.getAsJsonObject("token_exceptions"));
                slabFaces = mapOf(root.getAsJsonObject("slab_faces"));
                zhPathTokens = mapOf(root.getAsJsonObject("zh_path_tokens"));
                dyeColors = setOf(root.getAsJsonArray("dye_colors"));
            } catch (Exception failure) {
                throw new IllegalStateException(
                        "Could not load the language display contract",
                        failure);
            }
            loaded = true;
        }
    }

    private static Map<String, String> mapOf(JsonObject object) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (object == null) {
            return Map.of();
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(result);
    }

    private static Set<String> setOf(JsonArray array) {
        if (array == null) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (JsonElement entry : array) {
            result.add(entry.getAsString());
        }
        return Set.copyOf(result);
    }
}
