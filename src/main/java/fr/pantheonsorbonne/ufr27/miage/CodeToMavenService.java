package fr.pantheonsorbonne.ufr27.miage;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.io.MoreFiles;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CodeToMavenService {

    //stolen from https://stackoverflow.com/questions/65377062/javaparser-how-to-get-classname-in-compilationunit
    public static class ClassNameCollector extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<String> collector) {
            super.visit(n, collector);
            if (n.getModifiers().contains(Modifier.publicModifier())) {
                collector.add(n.getNameAsString());
            }
        }
    }

    public static ClassInfo getPackageFromCode(String code) {
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> compilationUnitParseResult = parser.parse(new StringReader(code));
        if (compilationUnitParseResult.isSuccessful()) {
            CompilationUnit compilationUnit = compilationUnitParseResult.getResult().orElseThrow();
            PackageDeclaration declaration = compilationUnit.getPackageDeclaration().orElseThrow();
            var packageName = declaration.getName().asString();
            VoidVisitor<List<String>> classNameVisitor = new ClassNameCollector();
            List<String> classNames = new ArrayList<>();
            classNameVisitor.visit(compilationUnit, classNames);
            if (classNames.size() == 0) {
                throw new RuntimeException("can't parse class Name");
            }

            return new ClassInfo(packageName, classNames.get(0));

        }
        throw new RuntimeException("can't parse java class");

    }

    public static InputStream getPath(String templateURI, String... codes) throws IOException, GitAPIException {
        java.nio.file.Path tmpPath = Files.createTempDirectory("tmp");
        try {
            System.out.println(tmpPath);
            Git.cloneRepository().setURI(templateURI).setDirectory(tmpPath.toFile()).call();
            //Git.cloneRepository().setURI("file:///home/nherbaut/tmp/java-maven-quickstart-latest.git").setDirectory(tmpPath.toFile()).call();


            for (String code : codes) {
                ClassInfo classInfo = getPackageFromCode(code);
                Path targetDirectory = Files.createDirectories(Path.of(tmpPath.toAbsolutePath().toString(),"src/main/java", classInfo.packageName().replace('.', '/')));
                Path javaFile = Path.of(targetDirectory.toString(), classInfo.className() + ".java");
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(javaFile.toFile()), StandardCharsets.UTF_8)) {
                    writer.write(code);
                }





            }
            MoreFiles.deleteRecursively(Paths.get(tmpPath.toString(), ".git"));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipUtil.pack(tmpPath.toFile(), bos);
            //well that sucks, but the code above only works locally.
            return new ByteArrayInputStream(bos.toByteArray());
        } finally {
            MoreFiles.deleteRecursively(tmpPath);
        }
    }

    public static void main(String... args) throws GitAPIException, IOException {
        if (Files.exists(Path.of("project.zip"))) {
            Files.delete(Path.of("project.zip"));
        }
        InputStream is = getPath("https://github.com/nherbaut/java-maven-quickstart-latest.git",
                """
                        package fr.miage;
                        public class Toto{}
                        """,
                """
                        package toto.miage;
                        public class Titi{}
                        """);
        File targetFile = new File("project.zip");
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        com.google.common.io.Files.write(buffer, targetFile);
    }
}

