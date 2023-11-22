package org.acme;

import org.eclipse.jgit.api.errors.GitAPIException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;

@Path("/maven")
public class CodeToMaven {

    @POST
    public Response post(String code) throws IOException, GitAPIException {


        InputStream is = CodeToMavenService.getPath(code);

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