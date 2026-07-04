package shipwrights.genesis.util;

/**
 * Minimal immutable pair, replacing the kotlin.Pair usages left over from the Valkyrien Skies era
 * (Genesis no longer depends on the Kotlin standard library).
 */
public final class Pair<A, B> {
    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }

    // Kotlin destructuring-compatible accessors used by ported code.
    public A component1() { return first; }
    public B component2() { return second; }
}
