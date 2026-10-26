package vn.camerahub.micro.dto;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuccessMessage {
    String value();
}
