package fr.pantheonsorbonne.ufr27.miage.resources;

import fr.pantheonsorbonne.ufr27.miage.service.CodeToMavenService;
import fr.pantheonsorbonne.ufr27.miage.model.FileType;
import fr.pantheonsorbonne.ufr27.miage.model.InjectedFile;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.jgit.api.errors.GitAPIException;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Path("/maven")
public class CodeToMavenResource {
    @POST
    public Response post(String code) throws IOException, GitAPIException {
        return post(Base64.getEncoder().withoutPadding().encodeToString("https://github.com/nherbaut/java-maven-quickstart-latest.git".getBytes(StandardCharsets.UTF_8)),
                List.of(new InjectedFile(FileType.SOURCE, code, "")));
    }

    @Path("{project-template}")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Response post(@PathParam("project-template") String template, List<InjectedFile> assets) throws IOException, GitAPIException {


        InputStream is = CodeToMavenService.getPath(new String(Base64.getDecoder().decode(template)), assets);

        StreamingOutput stream = output -> {
            try {
                is.transferTo(output);
            } catch (Exception e) {
                throw new WebApplicationException(e);
            } finally {
                is.close();
            }

        };


        return Response
                .ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename=\"filename.zip\"")
                .build();

    }


}