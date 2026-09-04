package com.masson.cruciblecraft.benchmark.recipe;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RuntimeSide;

/** Child-JVM endpoint retained while the parent invokes the JDK jcmd protocol. */
public final class RetainedMemoryChild {
    private static volatile RecipeFamilyProvider retainedProvider;
    private static volatile long retainedChecksum;

    private RetainedMemoryChild() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException(
                    "Expected candidate, compact path, relation count, "
                            + "hot modulo, epoch and lookup warmup");
        }
        String candidate = args[0];
        Path compactPath = Path.of(args[1]);
        int relationCount = Integer.parseInt(args[2]);
        int hotModulo = Integer.parseInt(args[3]);
        long epoch = Long.parseLong(args[4]);
        int lookupWarmup = Integer.parseInt(args[5]);

        List<ExtruderRelation> relations = ExtruderBenchmarkModel.load(
                compactPath, relationCount, hotModulo).relations();
        RecipeFamilyProvider provider =
                RecipeFamilyProviderContractTest.newProvider(candidate);
        provider.publish(relations, epoch, RuntimeSide.SERVER);
        LookupWorkloads.Trace trace = LookupWorkloads.build(
                        relations, Math.max(1, lookupWarmup))
                .traces().getFirst();
        retainedChecksum = exercise(
                provider, trace.operations());
        var diagnostics = provider.diagnostics();
        retainedProvider = provider;
        relations = null;
        provider = null;

        String gcNames = ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .map(bean -> bean.getName().replace('|', '/'))
                .sorted()
                .collect(java.util.stream.Collectors.joining(","));
        long loadedClasses = ManagementFactory.getClassLoadingMXBean()
                .getLoadedClassCount();
        long maxHeap = Runtime.getRuntime().maxMemory();
        long pid = ProcessHandle.current().pid();
        System.out.println(String.join(
                "|",
                "READY",
                Long.toString(pid),
                clean(System.getProperty("java.version")),
                clean(System.getProperty("java.vm.name")),
                clean(gcNames),
                Long.toString(maxHeap),
                Long.toString(loadedClasses),
                Long.toString(retainedChecksum),
                diagnostics.cachePolicy().name(),
                Integer.toString(diagnostics.cacheCeiling()),
                Integer.toString(diagnostics.cachedRecipes()),
                Long.toString(diagnostics.evictions())));
        System.out.flush();

        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in))) {
            input.readLine();
        }
        retainedProvider = null;
    }

    private static long exercise(
            RecipeFamilyProvider provider,
            List<ExtruderRelation> trace) {
        long checksum = 0L;
        for (ExtruderRelation relation : trace) {
            var recipe = provider.lookup(
                    ExtruderBenchmarkModel.request(relation)).orElseThrow();
            checksum += recipe.stableId().hashCode();
        }
        return checksum;
    }

    private static String clean(String value) {
        return String.valueOf(value)
                .replace('|', '/')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }
}
