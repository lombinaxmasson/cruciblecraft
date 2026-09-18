package com.masson.cruciblecraft.energy.cable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leftover {@code electric_wire/*} catalog identities. Live hosts use
 * {@code gold/wire}; these rows stay registered after fold.
 */
public final class ElectricWireRemainderIds {
    private static final Pattern PATH = Pattern.compile(
            "^electric_wire/(\\d+)x_(.+?)_(wire|cable)$");

    private ElectricWireRemainderIds() {}

    public record Parsed(
            int strands,
            String materialId,
            boolean cable,
            String specification) {}

    public static Optional<Parsed> parse(String registryPath) {
        if (registryPath == null || registryPath.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = PATH.matcher(registryPath);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int strands = Integer.parseInt(matcher.group(1));
        boolean cable = "cable".equals(matcher.group(3));
        String specification = specification(strands, cable);
        if (specification == null) {
            return Optional.empty();
        }
        return Optional.of(new Parsed(
                strands,
                matcher.group(2),
                cable,
                specification));
    }

    public static String itemModelParent(Parsed parsed) {
        return "conductor/"
                + parsed.specification().toLowerCase()
                + "_item";
    }

    private static String specification(int strands, boolean cable) {
        if (cable) {
            return switch (strands) {
                case 1 -> "cableGt01";
                case 2 -> "cableGt02";
                case 4 -> "cableGt04";
                case 8 -> "cableGt08";
                case 12 -> "cableGt12";
                default -> null;
            };
        }
        if (strands < 1 || strands > 16) {
            return null;
        }
        return String.format("wireGt%02d", strands);
    }
}
