package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.HelloWorldEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController implements HelloWorldEndpoint {

  @Autowired
  public HelloWorldController() {}


  @Override
  public String helloWorld() {
    return "Hello World";
  }
}
