package fr.pantheonsorbonne.ufr27.miage.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.io.MoreFiles;
import fr.pantheonsorbonne.ufr27.miage.model.ClassInfo;
import fr.pantheonsorbonne.ufr27.miage.model.FileType;
import fr.pantheonsorbonne.ufr27.miage.model.InjectedFile;
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
            //super.visit(n, collector);
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
            var packageName = "";
            if (compilationUnit.getPackageDeclaration().isPresent()) {
                packageName = compilationUnit.getPackageDeclaration().get().getName().asString();
            }

            VoidVisitor<List<String>> classNameVisitor = new ClassNameCollector();
            List<String> classNames = new ArrayList<>();
            classNameVisitor.visit(compilationUnit, classNames);
            if (classNames.isEmpty()) {
                throw new RuntimeException("can't parse class Name");
            }

            return new ClassInfo(packageName, classNames.getFirst());

        }
        throw new RuntimeException("can't parse java class");

    }

    public static InputStream getPath(String templateURI, List<InjectedFile> assets) throws IOException, GitAPIException {
        java.nio.file.Path tmpPath = Files.createTempDirectory("tmp");
        try {
            System.out.println(tmpPath);
            try (Git git = Git.cloneRepository().setURI(templateURI).setDirectory(tmpPath.toFile()).call()) {
                System.out.println("template repo cloned successfully");
            }



            assets.stream().filter(f -> f.fileType().equals(FileType.SOURCE)).map(InjectedFile::fileContent).forEach(code -> {
                ClassInfo classInfo = getPackageFromCode(code);
                Path targetDirectory = null;
                try {
                    targetDirectory = Files.createDirectories(Path.of(tmpPath.toAbsolutePath().toString(), "src/main/java", classInfo.packageName().replace('.', '/')));
                    Path javaFile = Path.of(targetDirectory.toString(), classInfo.className() + ".java");
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(javaFile.toFile()), StandardCharsets.UTF_8)) {
                        writer.write(code);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            assets.stream().filter(f -> f.fileType().equals(FileType.ASSET)).forEach(resourceSourceFile -> {

                Path targetDirectory = null;
                try {
                    targetDirectory = Files.createDirectories(Path.of(tmpPath.toAbsolutePath().toString(), "src/main/resources"));
                    Path resourceFilePath = Path.of(targetDirectory.toString(), resourceSourceFile.fileName());
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(resourceFilePath.toFile()), StandardCharsets.UTF_8)) {
                        writer.write(resourceSourceFile.fileContent());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });


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
        InputStream is = getPath("https://github.com/UFR27/2023-L2-POO-template.git",
                List.of(new InjectedFile(FileType.SOURCE,
                                """
                                        package fr.miage;
                                        public class Toto{}
                                        """, ""),
                        new InjectedFile(FileType.SOURCE, """
                                package toto.miage;
                                public class Titi{}
                                """, ""),
                        new InjectedFile(FileType.ASSET, """
                                sdfqsopdifjqposdifq
                                """, "scrapped.txt")));
        Path targetPAth = Path.of(Files.createTempDirectory("scrapper").toAbsolutePath().toString(), "project.zip");

        byte[] buffer = new byte[is.available()];
        int read_bytes=is.read(buffer);
        if(read_bytes!=0) {
            com.google.common.io.Files.write(buffer, targetPAth.toFile());
            System.out.println("checkout " + targetPAth);
        }
        else{
            throw new RuntimeException("can't parse java class");
        }
    }
}

