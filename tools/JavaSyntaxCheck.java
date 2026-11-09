import com.sun.source.util.JavacTask;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parse Java source without resolving Android dependencies. */
public final class JavaSyntaxCheck {
    private static void findJava(File folder, List<File> result) {
        File[] children = folder.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) findJava(f, result);
            else if (f.getName().endsWith(".java")) result.add(f);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass source root");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler not available");
        List<File> sources = new ArrayList<>();
        findJava(new File(args[0]), sources);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none"), null, manager.getJavaFileObjectsFromFiles(sources));
            task.parse();
            int errors = 0;
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors++;
                    System.err.println(d);
                }
            }
            if (errors > 0) throw new AssertionError(errors + " Java syntax error(s)");
        }
        System.out.println("PASS: Parsed " + sources.size() + " Java source files");
    }
}
