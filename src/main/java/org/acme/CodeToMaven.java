package org.acme;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.io.MoreFiles;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.zeroturnaround.zip.ZipUtil;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Path("/maven")
public class CodeToMaven {

    @POST
    public Response post(String code) throws IOException, GitAPIException {

        java.nio.file.Path tmpPath = Files.createTempDirectory("tmp");
        java.nio.file.Path tmpPath2 = Files.createTempDirectory("tmp2");
        Git.cloneRepository().setURI("https://github.com/nherbaut/java-maven-quickstart-latest.git").setDirectory(tmpPath.toFile()).call();
        //Git.cloneRepository().setURI("file:///home/nherbaut/tmp/java-maven-quickstart-latest.git").setDirectory(tmpPath.toFile()).call();
        java.nio.file.Path codeoutputFile = java.nio.file.Path.of(tmpPath.toString(), "src/main/java/Dummy.java");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(codeoutputFile.toFile()), StandardCharsets.UTF_8)) {
            writer.write(code);
        }


        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> cu = parser.parse(codeoutputFile);
        List<Node> cuChildren = cu.getResult().orElseThrow().getChildNodes();
        if (cuChildren == null || cuChildren.size() == 0 || ! (cuChildren.get(0) instanceof ClassOrInterfaceDeclaration)) {
            throw new RuntimeException("We cannot determine the file's class name, because the java file didn't parse");
        }

        String tipeName = ((ClassOrInterfaceDeclaration) cuChildren.get(0)).getNameAsString();
        Files.move(codeoutputFile, codeoutputFile.resolveSibling(tipeName + ".java"));
        MoreFiles.deleteRecursively(Paths.get(tmpPath.toString(), ".git"));
        ZipUtil.pack(tmpPath.toFile(), Paths.get(tmpPath2.toString(), "result.zip").toFile());


        return Response
                .ok(Paths.get(tmpPath2.toString(), "result.zip").toFile(), MediaType.APPLICATION_OCTET_STREAM)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename=\"filename.zip\"")
                .build();


    }

    ;
}