package bio.terra.cbas.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/")
public class SwaggerController {

  @GetMapping({"/", "swagger-ui"})
  public RedirectView getSwagger(RedirectAttributes redirectAttributes) {
    return new RedirectView("swagger-ui.html");
  }
}
