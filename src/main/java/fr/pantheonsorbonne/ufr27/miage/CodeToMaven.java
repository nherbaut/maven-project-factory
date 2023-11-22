package fr.pantheonsorbonne.ufr27.miage;

import org.eclipse.jgit.api.errors.GitAPIException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Path("/maven")
public class CodeToMaven {
    @POST
    public Response post(String code) throws IOException, GitAPIException {
        return post(Base64.getEncoder().withoutPadding().encodeToString("https://github.com/nherbaut/java-maven-quickstart-latest.git".getBytes(StandardCharsets.UTF_8)));
    }

    @Path("{project-template}")
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Response post(@PathParam("project-template") String template, List<String> codes) throws IOException, GitAPIException {


        InputStream is = CodeToMavenService.getPath(new String(Base64.getDecoder().decode(template)), codes.toArray(new String[0]));

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