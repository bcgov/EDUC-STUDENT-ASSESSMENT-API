package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(URL.BASE_URL + "/hello")
public interface HelloWorldEndpoint {

  @GetMapping()
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  String helloWorld();
}
