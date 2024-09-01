package fr.pantheonsorbonne.ufr27.miage;

import fr.pantheonsorbonne.ufr27.miage.model.ClassInfo;
import fr.pantheonsorbonne.ufr27.miage.service.CodeToMavenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeToMavenServiceTest {

    @Test
    void getPackageFromCode() {

        ClassInfo classInfo = CodeToMavenService.getPackageFromCode("""
                package fr.miage;
                public class Toto{
                 static class Titi{}
                 public static void main(String ...args){
                 var f = new Function<String,String>(){
                                                
                                                            @Override
                                                            public String apply(String s) {
                                                                return null;
                                                            }
                                                        };
                 }
                }
                """);
        assertEquals(new ClassInfo("fr.miage", "Toto"), classInfo);
    }
}