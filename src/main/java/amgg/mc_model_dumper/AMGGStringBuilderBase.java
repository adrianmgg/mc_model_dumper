package amgg.mc_model_dumper;

import org.intellij.lang.annotations.PrintFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AMGGStringBuilderBase<T extends AMGGStringBuilderBase<T>> {
    //        private static final Formatter formatter = new Formatter(Locale.ENGLISH);
//        private final List<String> chunks = new ArrayList<>();
    protected final Stack<String> indentStack = new Stack<>();
    protected @Nullable
    String currentIndent = null;
    protected String defaultIndent = "\t";
    protected final Stack<String> bracketStack = new Stack<>();
    protected final StringBuilder builder = new StringBuilder();

    protected abstract T self();

    public T add(@Nullable String s) {
        _add(String.valueOf(s));
        return self();
    }

    public T add(@Nullable Object o) {
        _add(String.valueOf(o));
        return self();
    }

    public T addIf(boolean condition, String s) {
        if (condition) {
            add(s);
        }
        return self();
    }

    public T addIf(boolean condition, Object o) {
        if (condition) {
            add(o);
        }
        return self();
    }

    public T addFormat(@PrintFormat String format, Object... args) {
        // TODO this will use default locale - do we actually want that?
        _add(String.format(format, args));
        return self();
    }

    public T addFormatIf(boolean condition, @PrintFormat String format, Object... args) {
        if (condition) return addFormat(format, args);
        return self();
    }

    public <T2> T forEach(@Nullable Iterable<T2> it, @Nonnull BiConsumer<T, T2> func) {
        if (it != null) it.forEach(t -> func.accept(self(), t));
        return self();
    }

    public <T2> T forEachDelimited(@Nullable Iterable<T2> it, @Nonnull String delimiter, @Nonnull BiConsumer<T, T2> func) {
        if (it != null) {
            boolean first = true;
            for (T2 t : it) {
                if (first) first = false;
                else add(delimiter);
                func.accept(self(), t);
            }
        }
        return self();
    }

    public <T2> T forEachIf(boolean condition, Iterable<T2> it, BiConsumer<T, T2> func) {
        if (condition) forEach(it, func);
        return self();
    }

    public <T2> T forEachIfDelimited(boolean condition, Iterable<T2> it, String delimiter, BiConsumer<T, T2> func) {
        if (condition) forEachDelimited(it, delimiter, func);
        return self();
    }

    public T ifDo(boolean condition, Consumer<T> func) {
        if (condition) func.accept(self());
        return self();
    }

    // ==== indent handling stuff ====
    private void updateIndent() {
        if (indentStack.empty()) currentIndent = null;
        else currentIndent = String.join("", indentStack);
    }

    private void _add(String s) {
        if (!s.contains("\n")) builder.append(s);
        else { // "\n" in s
            int endidx = 0, startidx = 0;
            while ((endidx = s.indexOf('\n', endidx)) >= 0) {
                builder.append(s, startidx, endidx + 1);
                if (currentIndent != null) builder.append(currentIndent);
                startidx = endidx = endidx + 1;
            }
            builder.append(s, startidx, s.length());
        }
    }

    /**
     * set default indent for all later calls to {@link #pushIndent()}
     *
     * @param indent default indent level
     * @see #pushIndent
     */
    public T setIndent(String indent) {
        defaultIndent = indent;
        return self();
    }

    /**
     * push a new custom indent string to the indent stack.
     *
     * @param indent indent string
     * @see #pushIndent()
     */
    public T pushIndent(String indent) {
        indentStack.push(indent);
        updateIndent();
        return self();
    }

    /**
     * push default indent (set by {@link #setIndent(String)} to indent stack
     *
     * @see #setIndent
     * @see #pushIndent(String)
     */
    public T pushIndent() {
        indentStack.push(defaultIndent);
        updateIndent();
        return self();
    }

    /**
     * pop one level of indent from the indent stack
     *
     * @throws EmptyStackException if indent stack is already empty
     * @see #clearIndent
     */
    public T popIndent() throws EmptyStackException {
        indentStack.pop();
        updateIndent();
        return self();
    }

    /**
     * clear indent stack
     *
     * @see #popIndent
     */
    public T clearIndent() {
        indentStack.clear();
        updateIndent();
        return self();
    }

    // ==== bracketing stuff ====

    /**
     * immediately add left bracket, and push right bracket to bracket stack to be added later
     *
     * @param left  left bracket
     * @param right right bracket
     * @see #closeBracket
     */
    public T bracket(String left, String right) {
        _add(left);
        bracketStack.push(right);
        return self();
    }

    /**
     * {@link #bracket} helper for parentheses/round brackets
     */
    public T bracketRound() {
        return bracket("(", ")");
    }

    /**
     * {@link #bracket} helper for chevrons/angle brackets
     */
    public T bracketSquare() {
        return bracket("[", "]");
    }

    /**
     * {@link #bracket} helper for square brackets
     */
    public T bracketCurly() {
        return bracket("{", "}");
    }

    /**
     * {@link #bracket} helper for braces/curly brackets
     */
    public T bracketAngle() {
        return bracket("<", ">");
    }

    /**
     * pop a bracket from the bracket stack and add it
     *
     * @throws EmptyStackException if bracket stack is already empty
     * @see #closeAllBrackets
     */
    public T closeBracket() throws EmptyStackException {
        _add(bracketStack.pop());
        return self();
    }

    /**
     * pop and add all remaining brackets from the bracket stack
     *
     * @see #closeBracket
     */
    public T closeAllBrackets() {
        while (!bracketStack.empty()) _add(bracketStack.pop());
        return self();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public String finish() {
        closeAllBrackets();
        clearIndent();
        return toString();
    }

    // TODO add version with default charset (what should the default be?)
    public void finishIntoFile(Path targetPath, Charset charset) throws IOException {
        Files.write(targetPath, finish().getBytes(charset));
    }
}
